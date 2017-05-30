// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.serviceuser;

import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_CREATED_BY;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_OWNER;

import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.config.AnonymousCowardName;
import com.google.gerrit.server.git.NotesBranchUtil;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;
import java.io.IOException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.notes.NoteMap;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CreateServiceUserNotes {
  private static final Logger log = LoggerFactory.getLogger(CreateServiceUserNotes.class);

  interface Factory {
    CreateServiceUserNotes create(Project.NameKey project, Repository git);
  }

  private static final String REFS_NOTES_SERVICEUSER = "refs/notes/serviceuser";

  private final PersonIdent gerritServerIdent;
  private final NotesBranchUtil.Factory notesBranchUtilFactory;
  private final ServiceUserResolver serviceUserResolver;
  private final @AnonymousCowardName String anonymousCowardName;
  private final Project.NameKey project;
  private final Repository git;

  private ObjectInserter inserter;
  private NoteMap serviceUserNotes;
  private StringBuilder message;

  @Inject
  CreateServiceUserNotes(
      @GerritPersonIdent PersonIdent gerritIdent,
      NotesBranchUtil.Factory notesBranchUtilFactory,
      ServiceUserResolver serviceUserResolver,
      @AnonymousCowardName String anonymousCowardName,
      @Assisted Project.NameKey project,
      @Assisted Repository git) {
    this.gerritServerIdent = gerritIdent;
    this.notesBranchUtilFactory = notesBranchUtilFactory;
    this.serviceUserResolver = serviceUserResolver;
    this.anonymousCowardName = anonymousCowardName;
    this.project = project;
    this.git = git;
  }

  void createNotes(String branch, ObjectId oldObjectId, ObjectId newObjectId)
      throws IOException, OrmException {
    if (ObjectId.zeroId().equals(newObjectId)) {
      return;
    }

    try (RevWalk rw = new RevWalk(git)) {
      try {
        RevCommit n = rw.parseCommit(newObjectId);
        rw.markStart(n);
        if (n.getParentCount() == 1 && n.getParent(0).equals(oldObjectId)) {
          rw.markUninteresting(rw.parseCommit(oldObjectId));
        } else {
          markUninteresting(git, branch, rw, oldObjectId);
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        return;
      }

      for (RevCommit c : rw) {
        ServiceUserInfo serviceUser = serviceUserResolver.getAsServiceUser(c.getCommitterIdent());
        if (serviceUser != null) {
          ObjectId content = createNoteContent(branch, serviceUser);
          getNotes().set(c, content);
          getMessage().append("* ").append(c.getShortMessage()).append("\n");
        }
      }
    }
  }

  void commitNotes() throws IOException, ConcurrentRefUpdateException {
    try {
      if (serviceUserNotes == null) {
        return;
      }

      message.insert(0, "Update notes for service user commits\n\n");
      notesBranchUtilFactory
          .create(project, git, inserter)
          .commitAllNotes(
              serviceUserNotes, REFS_NOTES_SERVICEUSER, gerritServerIdent, message.toString());
    } finally {
      if (inserter != null) {
        inserter.close();
      }
    }
  }

  private void markUninteresting(Repository git, String branch, RevWalk rw, ObjectId oldObjectId) {
    for (Ref r : git.getAllRefs().values()) {
      try {
        if (r.getName().equals(branch)) {
          if (!ObjectId.zeroId().equals(oldObjectId)) {
            // For the updated branch the oldObjectId is the tip of uninteresting
            // commit history
            rw.markUninteresting(rw.parseCommit(oldObjectId));
          }
        } else if (r.getName().startsWith(Constants.R_HEADS)
            || r.getName().startsWith(Constants.R_TAGS)) {
          rw.markUninteresting(rw.parseCommit(r.getObjectId()));
        }
      } catch (IOException e) {
        // skip if not parseable as a commit
      }
    }
  }

  private ObjectId createNoteContent(String branch, ServiceUserInfo serviceUser)
      throws IOException, OrmException {
    return getInserter()
        .insert(Constants.OBJ_BLOB, createServiceUserNote(branch, serviceUser).getBytes("UTF-8"));
  }

  private String createServiceUserNote(String branch, ServiceUserInfo serviceUser)
      throws OrmException {
    HeaderFormatter fmt = new HeaderFormatter(gerritServerIdent.getTimeZone(), anonymousCowardName);
    fmt.appendDate();
    fmt.append("Project", project.get());
    fmt.append("Branch", branch);
    fmt.appendUser(KEY_CREATED_BY, serviceUser.createdBy);
    for (AccountInfo owner : serviceUserResolver.listActiveOwners(serviceUser)) {
      fmt.appendUser(KEY_OWNER, owner);
    }
    return fmt.toString();
  }

  private ObjectInserter getInserter() {
    if (inserter == null) {
      inserter = git.newObjectInserter();
    }
    return inserter;
  }

  private NoteMap getNotes() {
    if (serviceUserNotes == null) {
      serviceUserNotes = NoteMap.newEmptyMap();
    }
    return serviceUserNotes;
  }

  private StringBuilder getMessage() {
    if (message == null) {
      message = new StringBuilder();
    }
    return message;
  }
}

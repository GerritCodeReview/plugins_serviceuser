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

import com.google.gerrit.extensions.restapi.MethodNotAllowedException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.AccountProjectWatch;
import com.google.gerrit.reviewdb.client.Change.Id;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountInfo;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.account.GroupMembership;
import com.google.gerrit.server.config.AnonymousCowardName;
import com.google.gerrit.server.git.NotesBranchUtil;
import com.google.gerrit.server.group.ListMembers;
import com.google.gerrit.server.util.RequestContext;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class CreateServiceUserNotes {
  private static final Logger log =
      LoggerFactory.getLogger(CreateServiceUserNotes.class);

  interface Factory {
    CreateServiceUserNotes create(Project.NameKey project, Repository git);
  }

  private static final String REFS_NOTES_SERVICEUSER = "refs/notes/serviceuser";

  private final PersonIdent gerritServerIdent;
  private final NotesBranchUtil.Factory notesBranchUtilFactory;
  private final AccountResolver resolver;
  private final IdentifiedUser.GenericFactory genericUserFactory;
  private final Provider<GetServiceUser> getServiceUser;
  private final Provider<ListMembers> listMembers;
  private final @AnonymousCowardName String anonymousCowardName;
  private final ThreadLocalRequestContext tl;
  private final SchemaFactory<ReviewDb> schema;
  private final Project.NameKey project;
  private final Repository git;

  private ObjectInserter inserter;
  private NoteMap serviceUserNotes;
  private StringBuilder message;

  @Inject
  CreateServiceUserNotes(@GerritPersonIdent PersonIdent gerritIdent,
      NotesBranchUtil.Factory notesBranchUtilFactory,
      AccountResolver resolver,
      IdentifiedUser.GenericFactory genericUserFactory,
      Provider<GetServiceUser> getServiceUser,
      Provider<ListMembers> listMembers,
      @AnonymousCowardName String anonymousCowardName,
      ThreadLocalRequestContext tl,
      SchemaFactory<ReviewDb> schema,
      @Assisted Project.NameKey project,
      @Assisted Repository git) {
    this.gerritServerIdent = gerritIdent;
    this.notesBranchUtilFactory = notesBranchUtilFactory;
    this.resolver = resolver;
    this.genericUserFactory = genericUserFactory;
    this.getServiceUser = getServiceUser;
    this.listMembers = listMembers;
    this.anonymousCowardName = anonymousCowardName;
    this.tl = tl;
    this.schema = schema;
    this.project = project;
    this.git = git;
  }

  void createNotes(String branch, ObjectId oldObjectId, ObjectId newObjectId)
      throws IOException, OrmException {
    if (ObjectId.zeroId().equals(newObjectId)) {
      return;
    }

    RevWalk rw = new RevWalk(git);
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

    try {
      for (RevCommit c : rw) {
        ServiceUserInfo serviceUser = getAsServiceUser(c.getCommitterIdent());
        if (serviceUser != null) {
          ObjectId content = createNoteContent(branch, serviceUser);
          getNotes().set(c, content);
          getMessage().append("* ").append(c.getShortMessage()).append("\n");
        }
      }
    } finally {
      rw.release();
    }
  }

  private ServiceUserInfo getAsServiceUser(PersonIdent committerIdent)
      throws OrmException {
    StringBuilder committer = new StringBuilder();
    committer.append(committerIdent.getName());
    committer.append(" <");
    committer.append(committerIdent.getEmailAddress());
    committer.append("> ");

    Account account = resolver.find(committer.toString());
    if (account == null) {
      return null;
    }
    try {
      return getServiceUser.get().apply(
          new ServiceUserResource(genericUserFactory.create(account.getId())));
    } catch (ResourceNotFoundException e) {
      return null;
    }
  }

  void commitNotes() throws IOException, ConcurrentRefUpdateException {
    try {
      if (serviceUserNotes == null) {
        return;
      }

      message.insert(0, "Update notes for service user commits\n\n");
      notesBranchUtilFactory.create(project, git, inserter)
          .commitAllNotes(serviceUserNotes, REFS_NOTES_SERVICEUSER, gerritServerIdent,
              message.toString());
    } finally {
      if (inserter != null) {
        inserter.release();
      }
    }
  }

  private void markUninteresting(Repository git, String branch, RevWalk rw,
      ObjectId oldObjectId) {
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
    return getInserter().insert(Constants.OBJ_BLOB,
        createServiceUserNote(branch, serviceUser).getBytes("UTF-8"));
  }

  private String createServiceUserNote(String branch,
      ServiceUserInfo serviceUser) throws OrmException {
    HeaderFormatter fmt =
        new HeaderFormatter(gerritServerIdent.getTimeZone(),
            anonymousCowardName);
    fmt.appendDate();
    fmt.append("Project", project.get());
    fmt.append("Branch", branch);
    fmt.appendUser(KEY_CREATED_BY, serviceUser.createdBy);
    for (AccountInfo owner : listOwners(serviceUser)) {
      fmt.appendUser(KEY_OWNER, owner);
    }
    return fmt.toString();
  }

  private List<AccountInfo> listOwners(ServiceUserInfo serviceUser) throws OrmException {
    if (serviceUser.owner == null) {
      return Collections.emptyList();
    }

    final ReviewDb db = schema.open();
    try {
      RequestContext context = new RequestContext() {
        @Override
        public CurrentUser getCurrentUser() {
          return new CurrentUser(null) {

            @Override
            public Set<Id> getStarredChanges() {
              return null;
            }

            @Override
            public Collection<AccountProjectWatch> getNotificationFilters() {
              return null;
            }

            @Override
            public GroupMembership getEffectiveGroups() {
              return new GroupMembership() {
                @Override
                public Set<AccountGroup.UUID> intersection(Iterable<AccountGroup.UUID> groupIds) {
                  return null;
                }

                @Override
                public Set<AccountGroup.UUID> getKnownGroups() {
                  return null;
                }

                @Override
                public boolean containsAnyOf(Iterable<AccountGroup.UUID> groupIds) {
                  return true;
                }

                @Override
                public boolean contains(AccountGroup.UUID groupId) {
                  return true;
                }
              };
            }
          };
        }

        @Override
        public Provider<ReviewDb> getReviewDbProvider() {
          return new Provider<ReviewDb>() {
            @Override
            public ReviewDb get() {
              return db;
            }};
        }
      };
      RequestContext old = tl.setContext(context);
      try {
        ListMembers lm = listMembers.get();
        lm.setRecursive(true);
        List<AccountInfo> owners = new ArrayList<>();
        for (AccountInfo a : lm.apply(new AccountGroup.UUID(serviceUser.owner.id))) {
          owners.add(a);
        }
        return owners;
      } catch (MethodNotAllowedException e) {
        log.error(String.format("Failed to list members of owner group %s for service user %s.",
            serviceUser.owner.name, serviceUser.username));
        return Collections.emptyList();
      } finally {
        tl.setContext(old);
      }
    } finally {
      db.close();
    }
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

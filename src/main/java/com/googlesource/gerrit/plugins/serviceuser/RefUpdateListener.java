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

import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.ProjectRunnable;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class RefUpdateListener implements GitReferenceUpdatedListener {
  private static final Logger log = LoggerFactory.getLogger(RefUpdateListener.class);

  private final CreateServiceUserNotes.Factory serviceUserNotesFactory;
  private final GitRepositoryManager repoManager;
  private final WorkQueue workQueue;
  private final PluginConfigFactory cfgFactory;
  private final String pluginName;

  @Inject
  RefUpdateListener(
      CreateServiceUserNotes.Factory serviceUserNotesFactory,
      GitRepositoryManager repoManager,
      WorkQueue workQueue,
      PluginConfigFactory cfgFactory,
      @PluginName String pluginName) {
    this.serviceUserNotesFactory = serviceUserNotesFactory;
    this.repoManager = repoManager;
    this.workQueue = workQueue;
    this.cfgFactory = cfgFactory;
    this.pluginName = pluginName;
  }

  @Override
  public void onGitReferenceUpdated(final Event event) {
    PluginConfig cfg = cfgFactory.getFromGerritConfig(pluginName);
    if (!cfg.getBoolean("createNotes", true)) {
      return;
    }

    Runnable task =
        new ProjectRunnable() {
          @Override
          public void run() {
            createServiceUserNotes(event);
          }

          @Override
          public Project.NameKey getProjectNameKey() {
            return Project.nameKey(event.getProjectName());
          }

          @Override
          public String getRemoteName() {
            return null;
          }

          @Override
          public boolean hasCustomizedPrint() {
            return true;
          }

          @Override
          public String toString() {
            return "create-service-user-notes";
          }
        };
    if (cfg.getBoolean("createNotesAsync", false)) {
      workQueue.getDefaultQueue().submit(task);
    } else {
      task.run();
    }
  }

  private void createServiceUserNotes(Event e) {
    Project.NameKey projectName = Project.nameKey(e.getProjectName());
    try (Repository git = repoManager.openRepository(projectName)) {
      CreateServiceUserNotes crn = serviceUserNotesFactory.create(projectName, git);
      crn.createNotes(
          e.getRefName(),
          ObjectId.fromString(e.getOldObjectId()),
          ObjectId.fromString(e.getNewObjectId()));
      crn.commitNotes();
    } catch (IOException
        | ConfigInvalidException
        | PermissionBackendException
        | RestApiException x) {
      log.error(x.getMessage(), x);
    }
  }
}

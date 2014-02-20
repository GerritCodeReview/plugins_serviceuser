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

import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_OWNER;

import com.google.common.base.Strings;
import com.google.gerrit.common.data.GroupDescription;
import com.google.gerrit.common.data.GroupDescriptions;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.DefaultInput;
import com.google.gerrit.extensions.restapi.MethodNotAllowedException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.group.GroupJson.GroupInfo;
import com.google.gerrit.server.group.GroupJson;
import com.google.gerrit.server.group.GroupsCollection;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.googlesource.gerrit.plugins.serviceuser.PutOwner.Input;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Config;

import java.io.IOException;

public class PutOwner implements RestModifyView<ServiceUserResource, Input> {
  public static class Input {
    @DefaultInput
    public String group;
  }

  private final Provider<GetConfig> getConfig;
  private final GroupsCollection groups;
  private final ProjectLevelConfig storage;
  private final Project.NameKey allProjects;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final GroupJson json;
  private final Provider<CurrentUser> self;

  @Inject
  PutOwner(Provider<GetConfig> getConfig, GroupsCollection groups,
      @PluginName String pluginName, ProjectCache projectCache,
      MetaDataUpdate.User metaDataUpdateFactory, GroupJson json,
      Provider<CurrentUser> self) {
    this.getConfig = getConfig;
    this.groups = groups;
    this.storage = projectCache.getAllProjects().getConfig(pluginName + ".db");
    this.allProjects = projectCache.getAllProjects().getProject().getNameKey();
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.json = json;
    this.self = self;
  }

  @Override
  public Response<GroupInfo> apply(ServiceUserResource rsrc, Input input)
      throws UnprocessableEntityException, RepositoryNotFoundException,
      MethodNotAllowedException, IOException, OrmException, ResourceConflictException {
    Boolean ownerAllowed = getConfig.get().apply(new ConfigResource()).allowOwner;
    if ((ownerAllowed == null || !ownerAllowed)
        && !self.get().getCapabilities().canAdministrateServer()) {
      throw new ResourceConflictException("setting owner not allowed");
    }

    if (input == null) {
      input = new Input();

    }
    Config db = storage.get();
    String oldGroup = db.getString(USER, rsrc.getUser().getUserName(), KEY_OWNER);
    GroupDescription.Basic group = null;
    if (Strings.isNullOrEmpty(input.group)) {
      db.unset(USER, rsrc.getUser().getUserName(), KEY_OWNER);
    } else {
      group = groups.parse(input.group);
      if (GroupDescriptions.toAccountGroup(group) == null) {
        throw new MethodNotAllowedException();
      }
      db.setString(USER, rsrc.getUser().getUserName(), KEY_OWNER,
          group.getGroupUUID().get());
    }
    MetaDataUpdate md = metaDataUpdateFactory.create(allProjects);
    md.setMessage("Set owner for service user '" + rsrc.getUser().getUserName() + "'\n");
    storage.commit(md);
    return group != null
        ? (oldGroup != null ? Response.ok(json.format(group)) : Response.created(json.format(group)))
        : Response.<GroupInfo> none();
  }
}

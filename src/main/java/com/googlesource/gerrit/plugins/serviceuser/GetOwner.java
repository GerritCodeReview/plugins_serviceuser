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

import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_OWNER;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;

import com.google.gerrit.common.data.GroupDescription;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.common.GroupInfo;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.group.GroupJson;
import com.google.gerrit.server.group.GroupsCollection;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class GetOwner implements RestReadView<ServiceUserResource> {
  private final GroupsCollection groups;
  private final String pluginName;
  private final ProjectCache projectCache;
  private final GroupJson json;

  @Inject
  GetOwner(GroupsCollection groups, @PluginName String pluginName,
      ProjectCache projectCache, GroupJson json) {
    this.groups = groups;
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.json = json;
  }

  @Override
  public Response<GroupInfo> apply(ServiceUserResource rsrc)
      throws ResourceNotFoundException, OrmException {
    ProjectLevelConfig storage = projectCache.getAllProjects().getConfig(pluginName + ".db");
    String owner = storage.get().getString(USER, rsrc.getUser().getUserName(), KEY_OWNER);
    if (owner != null) {
      GroupDescription.Basic group = groups.parseId(owner);
      return Response.<GroupInfo> ok(json.format(group));
    }
    return Response.none();
  }
}

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
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectLevelConfig;
import com.google.gerrit.server.restapi.group.GroupJson;
import com.google.gerrit.server.restapi.group.GroupsCollection;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class GetOwner implements RestReadView<ServiceUserResource> {
  private final GroupsCollection groups;
  private final String pluginName;
  private final ProjectCache projectCache;
  private final GroupJson json;
  private final OneOffRequestContext requestContext;

  @Inject
  GetOwner(
      GroupsCollection groups,
      @PluginName String pluginName,
      ProjectCache projectCache,
      GroupJson json,
      OneOffRequestContext requestContext) {
    this.groups = groups;
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.json = json;
    this.requestContext = requestContext;
  }

  @Override
  public Response<GroupInfo> apply(ServiceUserResource rsrc)
      throws RestApiException, PermissionBackendException {
    try (ManualRequestContext ctx = requestContext.open()) {
      ProjectLevelConfig storage = projectCache.getAllProjects().getConfig(pluginName + ".db");
      String owner = storage.get().getString(USER, rsrc.getUser().getUserName().get(), KEY_OWNER);
      if (owner != null) {
        GroupDescription.Basic group =
            groups.parse(TopLevelResource.INSTANCE, IdString.fromDecoded(owner)).getGroup();
        return Response.<GroupInfo>ok(json.format(group));
      }
    }
    return Response.none();
  }
}

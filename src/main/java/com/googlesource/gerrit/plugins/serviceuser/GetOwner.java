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

import static com.google.gerrit.server.api.ApiUtil.asRestApiException;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_OWNER;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;

import com.google.gerrit.entities.GroupDescription;
import com.google.gerrit.extensions.common.GroupInfo;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.git.meta.MetaDataUpdate;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.ProjectLevelConfig;
import com.google.gerrit.server.restapi.group.GroupJson;
import com.google.gerrit.server.restapi.group.GroupsCollection;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;

@Singleton
class GetOwner implements RestReadView<ServiceUserResource> {
  private final GroupsCollection groups;
  private final GroupJson json;
  private final Provider<ProjectLevelConfig.Bare> configProvider;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final AllProjectsName allProjectsName;

  @Inject
  GetOwner(
      GroupsCollection groups,
      Provider<ProjectLevelConfig.Bare> configProvider,
      AllProjectsName allProjectsName,
      MetaDataUpdate.User metaDataUpdateFactory,
      GroupJson json) {
    this.groups = groups;
    this.configProvider = configProvider;
    this.allProjectsName = allProjectsName;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.json = json;
  }

  @Override
  public Response<GroupInfo> apply(ServiceUserResource rsrc)
      throws IOException, RestApiException, PermissionBackendException {
    ProjectLevelConfig.Bare storage = configProvider.get();
    try (MetaDataUpdate md = metaDataUpdateFactory.create(allProjectsName)) {
      storage.load(md);
    } catch (ConfigInvalidException e) {
      throw asRestApiException("Invalid configuration", e);
    }
    String owner =
        storage.getConfig().getString(USER, rsrc.getUser().getUserName().get(), KEY_OWNER);
    if (owner != null) {
      GroupDescription.Basic group =
          groups.parse(TopLevelResource.INSTANCE, IdString.fromDecoded(owner)).getGroup();
      return Response.<GroupInfo>ok(json.format(group));
    }
    return Response.none();
  }
}

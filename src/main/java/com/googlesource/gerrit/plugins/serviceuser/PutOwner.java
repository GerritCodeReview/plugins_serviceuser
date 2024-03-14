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
import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_OWNER;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;

import com.google.common.base.Strings;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.AccountGroup.UUID;
import com.google.gerrit.entities.GroupDescription;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.common.GroupInfo;
import com.google.gerrit.extensions.restapi.DefaultInput;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.MethodNotAllowedException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.git.meta.MetaDataUpdate;
import com.google.gerrit.server.git.meta.VersionedConfigFile;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.restapi.group.GroupJson;
import com.google.gerrit.server.restapi.group.GroupsCollection;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.PutOwner.Input;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;

@Singleton
class PutOwner implements RestModifyView<ServiceUserResource, Input> {
  public static class Input {
    @DefaultInput public String group;
  }

  private final Provider<GetConfig> getConfig;
  private final GroupsCollection groups;
  private final Provider<VersionedConfigFile> configProvider;
  private final Project.NameKey allProjects;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final GroupJson json;
  private final Provider<CurrentUser> self;
  private final PermissionBackend permissionBackend;
  private final StorageCache storageCache;

  @Inject
  PutOwner(
      Provider<GetConfig> getConfig,
      GroupsCollection groups,
      Provider<VersionedConfigFile> configProvider,
      ProjectCache projectCache,
      MetaDataUpdate.User metaDataUpdateFactory,
      GroupJson json,
      Provider<CurrentUser> self,
      PermissionBackend permissionBackend,
      StorageCache storageCache) {
    this.getConfig = getConfig;
    this.groups = groups;
    this.configProvider = configProvider;
    this.allProjects = projectCache.getAllProjects().getProject().getNameKey();
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.json = json;
    this.self = self;
    this.permissionBackend = permissionBackend;
    this.storageCache = storageCache;
  }

  @Override
  public Response<GroupInfo> apply(ServiceUserResource rsrc, Input input)
      throws RestApiException, IOException, PermissionBackendException {
    Boolean ownerAllowed;
    try {
      ownerAllowed = getConfig.get().apply(new ConfigResource()).value().allowOwner;
    } catch (Exception e) {
      throw asRestApiException("Cannot get configuration", e);
    }
    if ((ownerAllowed == null || !ownerAllowed)) {
      permissionBackend.user(self.get()).check(ADMINISTRATE_SERVER);
    }

    if (input == null) {
      input = new Input();
    }

    GroupDescription.Basic group = null;
    String oldGroup;
    try (MetaDataUpdate md = metaDataUpdateFactory.create(allProjects)) {
      VersionedConfigFile update = configProvider.get();
      update.load(md);

      Config db = update.getConfig();
      oldGroup = db.getString(USER, rsrc.getUser().getUserName().get(), KEY_OWNER);
      if (Strings.isNullOrEmpty(input.group)) {
        db.unset(USER, rsrc.getUser().getUserName().get(), KEY_OWNER);
      } else {
        group =
            groups.parse(TopLevelResource.INSTANCE, IdString.fromDecoded(input.group)).getGroup();
        UUID groupUUID = group.getGroupUUID();
        if (!AccountGroup.uuid(groupUUID.get()).isInternalGroup()) {
          throw new MethodNotAllowedException("Group with UUID '" + groupUUID + "' is external");
        }
        db.setString(USER, rsrc.getUser().getUserName().get(), KEY_OWNER, groupUUID.get());
      }
      md.setMessage("Set owner for service user '" + rsrc.getUser().getUserName() + "'\n");
      update.commit(md);
      storageCache.invalidate();
    } catch (ConfigInvalidException e) {
      throw asRestApiException("Invalid configuration", e);
    }

    return group != null
        ? (oldGroup != null
            ? Response.ok(json.format(group))
            : Response.created(json.format(group)))
        : Response.<GroupInfo>none();
  }
}

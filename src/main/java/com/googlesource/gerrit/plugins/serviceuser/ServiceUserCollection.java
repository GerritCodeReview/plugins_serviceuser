// Copyright (C) 2013 The Android Open Source Project
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

import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_CREATOR_ID;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_OWNER;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;

import com.google.gerrit.common.data.GroupDescription;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.AcceptsCreate;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.ChildCollection;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.restapi.account.AccountsCollection;
import com.google.gerrit.server.restapi.group.GroupsCollection;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;

@Singleton
class ServiceUserCollection
    implements ChildCollection<ConfigResource, ServiceUserResource>, AcceptsCreate<ConfigResource> {

  private final DynamicMap<RestView<ServiceUserResource>> views;
  private final CreateServiceUser.Factory createServiceUserFactory;
  private final Provider<ListServiceUsers> list;
  private final Provider<AccountsCollection> accounts;
  private final String pluginName;
  private final ProjectCache projectCache;
  private final Provider<CurrentUser> userProvider;
  private final GroupsCollection groups;
  private final PermissionBackend permissionBackend;

  @Inject
  ServiceUserCollection(
      DynamicMap<RestView<ServiceUserResource>> views,
      CreateServiceUser.Factory createServiceUserFactory,
      Provider<ListServiceUsers> list,
      Provider<AccountsCollection> accounts,
      @PluginName String pluginName,
      ProjectCache projectCache,
      Provider<CurrentUser> userProvider,
      GroupsCollection groups,
      PermissionBackend permissionBackend) {
    this.views = views;
    this.createServiceUserFactory = createServiceUserFactory;
    this.list = list;
    this.accounts = accounts;
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.userProvider = userProvider;
    this.groups = groups;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public ServiceUserResource parse(ConfigResource parent, IdString id)
      throws ResourceNotFoundException, AuthException, IOException, OrmException,
          PermissionBackendException, ConfigInvalidException {
    ProjectLevelConfig storage = projectCache.getAllProjects().getConfig(pluginName + ".db");
    IdentifiedUser serviceUser = accounts.get().parseId(id.get());
    if (serviceUser == null) {
      throw new ResourceNotFoundException(id);
    }

    Optional<String> username = serviceUser.getUserName();
    if (!username.isPresent()) {
      throw new ResourceNotFoundException("username doesn't exist");
    }

    if (!storage.get().getSubsections(USER).contains(username.get())) {
      throw new ResourceNotFoundException(id);
    }
    CurrentUser user = userProvider.get();
    if (user == null || !user.isIdentifiedUser()) {
      throw new AuthException("Authentication required");
    }
    if (!permissionBackend.user(userProvider).testOrFalse(ADMINISTRATE_SERVER)) {
      String owner = storage.get().getString(USER, id.get(), KEY_OWNER);
      if (owner != null) {
        try {
          GroupDescription.Basic group = groups.parse(owner);
          if (!user.getEffectiveGroups().contains(group.getGroupUUID())) {
            throw new ResourceNotFoundException(id);
          }
        } catch (UnprocessableEntityException e) {
          throw new ResourceNotFoundException(id);
        }
      } else if (!((IdentifiedUser) user)
          .getAccountId()
          .equals(new Account.Id(storage.get().getInt(USER, id.get(), KEY_CREATOR_ID, -1)))) {
        throw new ResourceNotFoundException(id);
      }
    }
    return new ServiceUserResource(serviceUser);
  }

  @Override
  public RestView<ConfigResource> list() {
    return list.get();
  }

  @Override
  public DynamicMap<RestView<ServiceUserResource>> views() {
    return views;
  }

  @Override
  public CreateServiceUser create(ConfigResource parent, IdString username) {
    return createServiceUserFactory.create(username.get());
  }
}

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

import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.GroupDescription;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.ChildCollection;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.restapi.account.AccountsCollection;
import com.google.gerrit.server.restapi.group.GroupsCollection;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;

@Singleton
class ServiceUserCollection implements ChildCollection<ConfigResource, ServiceUserResource> {

  private final DynamicMap<RestView<ServiceUserResource>> views;
  private final Provider<ListServiceUsers> list;
  private final Provider<AccountsCollection> accounts;
  private final Provider<CurrentUser> userProvider;
  private final GroupsCollection groups;
  private final PermissionBackend permissionBackend;
  private final StorageCache storageCache;

  @Inject
  ServiceUserCollection(
      DynamicMap<RestView<ServiceUserResource>> views,
      Provider<ListServiceUsers> list,
      Provider<AccountsCollection> accounts,
      Provider<CurrentUser> userProvider,
      GroupsCollection groups,
      PermissionBackend permissionBackend,
      StorageCache storageCache) {
    this.views = views;
    this.list = list;
    this.accounts = accounts;
    this.userProvider = userProvider;
    this.groups = groups;
    this.permissionBackend = permissionBackend;
    this.storageCache = storageCache;
  }

  @Override
  public ServiceUserResource parse(ConfigResource parent, IdString id)
      throws ResourceNotFoundException, AuthException, IOException, PermissionBackendException,
          ConfigInvalidException, RestApiException {
    IdentifiedUser serviceUser = accounts.get().parse(TopLevelResource.INSTANCE, id).getUser();
    Config db = storageCache.get();
    if (serviceUser == null || !db.getSubsections(USER).contains(serviceUser.getUserName().get())) {
      throw new ResourceNotFoundException(id);
    }
    CurrentUser user = userProvider.get();
    if (user == null || !user.isIdentifiedUser()) {
      throw new AuthException("Authentication required");
    }
    if (!permissionBackend.user(user).testOrFalse(ADMINISTRATE_SERVER)) {
      String username = serviceUser.getUserName().get();
      String owner = db.getString(USER, username, KEY_OWNER);
      if (owner != null) {
        GroupDescription.Basic group =
            groups.parse(TopLevelResource.INSTANCE, IdString.fromDecoded(owner)).getGroup();
        if (!user.getEffectiveGroups().contains(group.getGroupUUID())) {
          throw new ResourceNotFoundException(id);
        }
      } else if (!((IdentifiedUser) user)
          .getAccountId()
          .equals(Account.id(db.getInt(USER, username, KEY_CREATOR_ID, -1)))) {
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
}

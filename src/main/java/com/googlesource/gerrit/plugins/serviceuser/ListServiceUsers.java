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
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;

import com.google.common.collect.Maps;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;

@Singleton
class ListServiceUsers implements RestReadView<ConfigResource> {
  private final Provider<CurrentUser> userProvider;
  private final AccountCache accountCache;
  private final Provider<ServiceUserCollection> serviceUsers;
  private final Provider<GetServiceUser> getServiceUser;
  private final StorageCache storageCache;

  @Inject
  ListServiceUsers(
      Provider<CurrentUser> userProvider,
      AccountCache accountCache,
      Provider<ServiceUserCollection> serviceUsers,
      Provider<GetServiceUser> getServiceUser,
      StorageCache storageCache) {
    this.userProvider = userProvider;
    this.accountCache = accountCache;
    this.serviceUsers = serviceUsers;
    this.getServiceUser = getServiceUser;
    this.storageCache = storageCache;
  }

  @Override
  public Response<Map<String, ServiceUserInfo>> apply(ConfigResource rscr)
      throws IOException, RestApiException, PermissionBackendException, ConfigInvalidException {
    CurrentUser user = userProvider.get();
    if (user == null || !user.isIdentifiedUser()) {
      throw new AuthException("Authentication required");
    }

    Map<String, ServiceUserInfo> accounts = Maps.newTreeMap();
    Config db = storageCache.get();
    for (String username : db.getSubsections(USER)) {
      Optional<AccountState> account = accountCache.getByUsername(username);
      if (account.isPresent()) {
        ServiceUserInfo info;
        try {
          ServiceUserResource serviceUserResource =
              serviceUsers
                  .get()
                  .parse(
                      new ConfigResource(),
                      IdString.fromDecoded(String.valueOf(account.get().account().id().get())));
          info = getServiceUser.get().apply(serviceUserResource).value();
          info.username = null;
          accounts.put(username, info);
        } catch (ResourceNotFoundException e) {
          // this service user is not visible to the caller -> ignore it
        } catch (Exception e) {
          throw asRestApiException("Cannot list service users", e);
        }
      }
    }
    return Response.ok(accounts);
  }
}

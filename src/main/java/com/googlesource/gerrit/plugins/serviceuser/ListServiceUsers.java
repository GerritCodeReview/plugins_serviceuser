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

import com.google.common.collect.Maps;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;
import java.util.Map;
import org.eclipse.jgit.lib.Config;

@Singleton
class ListServiceUsers implements RestReadView<ConfigResource> {
  private final Provider<CurrentUser> userProvider;
  private final String pluginName;
  private final ProjectCache projectCache;
  private final AccountCache accountCache;
  private final Provider<ServiceUserCollection> serviceUsers;
  private final Provider<GetServiceUser> getServiceUser;

  @Inject
  ListServiceUsers(
      Provider<CurrentUser> userProvider,
      @PluginName String pluginName,
      ProjectCache projectCache,
      AccountCache accountCache,
      Provider<ServiceUserCollection> serviceUsers,
      Provider<GetServiceUser> getServiceUser) {
    this.userProvider = userProvider;
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.accountCache = accountCache;
    this.serviceUsers = serviceUsers;
    this.getServiceUser = getServiceUser;
  }

  @Override
  public Map<String, ServiceUserInfo> apply(ConfigResource rscr)
      throws OrmException, AuthException, PermissionBackendException {
    ProjectLevelConfig storage = projectCache.getAllProjects().getConfig(pluginName + ".db");
    CurrentUser user = userProvider.get();
    if (user == null || !user.isIdentifiedUser()) {
      throw new AuthException("Authentication required");
    }

    Map<String, ServiceUserInfo> accounts = Maps.newTreeMap();
    Config db = storage.get();
    for (String username : db.getSubsections(USER)) {
      AccountState account = accountCache.getByUsername(username);
      if (account != null) {
        ServiceUserInfo info;
        try {
          ServiceUserResource serviceUserResource =
              serviceUsers.get().parse(new ConfigResource(), IdString.fromDecoded(username));
          info = getServiceUser.get().apply(serviceUserResource);
          info.username = null;
          accounts.put(username, info);
        } catch (ResourceNotFoundException e) {
          // this service user is not visible to the caller -> ignore it
        }
      }
    }
    return accounts;
  }
}

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

import com.google.common.collect.Maps;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountInfo;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.account.GetAccount;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.eclipse.jgit.lib.Config;

import java.util.Map;

public class ListServiceUsers implements RestReadView<ConfigResource> {
  private final Provider<CurrentUser> userProvider;
  private final IdentifiedUser.GenericFactory userFactory;
  private final ProjectLevelConfig storage;
  private final AccountCache accountCache;
  private final Provider<GetAccount> getAccount;

  @Inject
  ListServiceUsers(Provider<CurrentUser> userProvider,
      IdentifiedUser.GenericFactory userFactory, @PluginName String pluginName,
      ProjectCache projectCache, AccountCache accountCache,
      Provider<GetAccount> getAccount) {
    this.userProvider = userProvider;
    this.userFactory = userFactory;
    this.storage = projectCache.getAllProjects().getConfig(pluginName + ".db");
    this.accountCache = accountCache;
    this.getAccount = getAccount;
  }

  @Override
  public Map<String, AccountInfo> apply(ConfigResource rscr) throws OrmException {
    Map<String, AccountInfo> accounts = Maps.newTreeMap();
    Config db = storage.get();
    for (String username : db.getSubsections(CreateServiceUser.USER)) {
      String createdBy =
          db.getString(CreateServiceUser.USER, username,
              CreateServiceUser.KEY_CREATED_BY);
      if (userProvider.get().getCapabilities().canAdministrateServer()
          || userProvider.get().getUserName().equals(createdBy)) {
        AccountState account = accountCache.getByUsername(username);
        if (account != null) {
          accounts.put(username, getAccount.get().apply(
              new AccountResource(userFactory.create(account.getAccount().getId()))));
        }
      }
    }
    return accounts;
  }
}

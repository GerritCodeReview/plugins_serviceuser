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

import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_CREATED_AT;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_CREATOR_ID;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.server.account.AccountInfo;
import com.google.gerrit.server.account.GetAccount;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.group.GroupJson.GroupInfo;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.jgit.lib.Config;

@Singleton
class GetServiceUser implements RestReadView<ServiceUserResource> {
  private final Provider<GetAccount> getAccount;
  private final ProjectLevelConfig storage;
  private final GetOwner getOwner;
  private final AccountInfo.Loader.Factory accountLoader;

  @Inject
  GetServiceUser(Provider<GetAccount> getAccount,
      @PluginName String pluginName, ProjectCache projectCache,
      GetOwner getOwner, AccountInfo.Loader.Factory accountLoader) {
    this.getAccount = getAccount;
    this.storage = projectCache.getAllProjects().getConfig(pluginName + ".db");
    this.getOwner = getOwner;
    this.accountLoader = accountLoader;
  }

  @Override
  public ServiceUserInfo apply(ServiceUserResource rsrc)
      throws ResourceNotFoundException, OrmException {
    String username = rsrc.getUser().getUserName();
    Config db = storage.get();
    if (!db.getSubsections(USER).contains(username)) {
      throw new ResourceNotFoundException(username);
    }

    ServiceUserInfo info = new ServiceUserInfo(getAccount.get().apply(rsrc));
    AccountInfo.Loader al = accountLoader.create(true);
    info.createdBy =
        al.get(new Account.Id(db.getInt(USER, username, KEY_CREATOR_ID, -1)));
    al.fill();
    info.createdAt = db.getString(USER, username, KEY_CREATED_AT);
    info.inactive = !rsrc.getUser().getAccount().isActive() ? true : null;

    Response<GroupInfo> response = getOwner.apply(rsrc);
    if (response.statusCode() == SC_OK) {
      info.owner = response.value();
    }

    return info;
  }

  public static class ServiceUserInfo extends AccountInfo {
    public AccountInfo createdBy;
    public String createdAt;
    public Boolean inactive;
    public GroupInfo owner;

    public ServiceUserInfo(AccountInfo info) {
      super(info._id);
      _accountId = info._accountId;
      name = info.name;
      email = info.email;
      username = info.username;
      avatars = info.avatars;
    }
  }
}

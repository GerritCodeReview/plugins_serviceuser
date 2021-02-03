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
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_CREATED_AT;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_CREATOR_ID;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.GroupInfo;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.account.AccountLoader;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.git.meta.MetaDataUpdate;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.ProjectLevelConfig;
import com.google.gerrit.server.restapi.account.GetAccount;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;

@Singleton
class GetServiceUser implements RestReadView<ServiceUserResource> {
  private final Provider<GetAccount> getAccount;
  private final GetOwner getOwner;
  private final AccountLoader.Factory accountLoader;
  private final Provider<ProjectLevelConfig.Bare> configProvider;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final AllProjectsName allProjectsName;

  @Inject
  GetServiceUser(
      Provider<GetAccount> getAccount,
      Provider<ProjectLevelConfig.Bare> configProvider,
      AllProjectsName allProjectsName,
      MetaDataUpdate.User metaDataUpdateFactory,
      GetOwner getOwner,
      AccountLoader.Factory accountLoader) {
    this.getAccount = getAccount;
    this.configProvider = configProvider;
    this.allProjectsName = allProjectsName;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.getOwner = getOwner;
    this.accountLoader = accountLoader;
  }

  @Override
  public Response<ServiceUserInfo> apply(ServiceUserResource rsrc)
      throws IOException, RestApiException, PermissionBackendException {
    ProjectLevelConfig.Bare storage = configProvider.get();
    try (MetaDataUpdate md = metaDataUpdateFactory.create(allProjectsName)) {
      storage.load(md);
    } catch (ConfigInvalidException e) {
      throw asRestApiException("Invalid configuration", e);
    }
    String username = rsrc.getUser().getUserName().get();
    Config db = storage.getConfig();
    if (!db.getSubsections(USER).contains(username)) {
      throw new ResourceNotFoundException(username);
    }

    ServiceUserInfo info;
    try {
      info = new ServiceUserInfo(getAccount.get().apply(rsrc).value());
    } catch (Exception e) {
      throw asRestApiException("Cannot get service user", e);
    }

    AccountLoader al = accountLoader.create(true);
    info.createdBy = al.get(Account.id(db.getInt(USER, username, KEY_CREATOR_ID, -1)));
    al.fill();
    info.createdAt = db.getString(USER, username, KEY_CREATED_AT);
    info.inactive = !rsrc.getUser().getAccount().isActive() ? true : null;

    Response<GroupInfo> response = getOwner.apply(rsrc);

    if (response.statusCode() == SC_OK) {
      try {
        info.owner = response.value();
      } catch (Exception e) {
        throw asRestApiException("Cannot get owner", e);
      }
    }

    return Response.ok(info);
  }

  public static class ServiceUserInfo extends AccountInfo {
    public AccountInfo createdBy;
    public String createdAt;
    public GroupInfo owner;

    public ServiceUserInfo(AccountInfo info) {
      super(info._accountId);
      _accountId = info._accountId;
      name = info.name;
      email = info.email;
      username = info.username;
      avatars = info.avatars;
      inactive = info.inactive;
    }
  }
}

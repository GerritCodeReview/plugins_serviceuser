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

import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_CREATED_BY;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.AcceptsCreate;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.ChildCollection;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.AnonymousUser;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountsCollection;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ServiceUserCollection implements
    ChildCollection<ConfigResource, ServiceUserResource>,
    AcceptsCreate<ConfigResource> {

  private final DynamicMap<RestView<ServiceUserResource>> views;
  private final CreateServiceUser.Factory createServiceUserFactory;
  private final Provider<ListServiceUsers> list;
  private final Provider<AccountsCollection> accounts;
  private final ProjectLevelConfig storage;
  private final Provider<CurrentUser> userProvider;

  @Inject
  ServiceUserCollection(DynamicMap<RestView<ServiceUserResource>> views,
      CreateServiceUser.Factory createServiceUserFactory,
      Provider<ListServiceUsers> list, Provider<AccountsCollection> accounts,
      @PluginName String pluginName, ProjectCache projectCache,
      Provider<CurrentUser> userProvider) {
    this.views = views;
    this.createServiceUserFactory = createServiceUserFactory;
    this.list = list;
    this.accounts = accounts;
    this.storage = projectCache.getAllProjects().getConfig(pluginName + ".db");
    this.userProvider = userProvider;
  }

  @Override
  public ServiceUserResource parse(ConfigResource parent, IdString id)
      throws ResourceNotFoundException, AuthException, OrmException {
    if (!storage.get().getSubsections(USER).contains(id.get())) {
      throw new ResourceNotFoundException(id);
    }
    CurrentUser user = userProvider.get();
    if (user instanceof AnonymousUser) {
      throw new AuthException("Authentication required");
    }
    if (!user.getUserName().equals(
        storage.get().getString(USER, id.get(), KEY_CREATED_BY))
        && !user.getCapabilities().canAdministrateServer()) {
      throw new ResourceNotFoundException(id);
    }
    return new ServiceUserResource(
        accounts.get().parse(TopLevelResource.INSTANCE, id).getUser());
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
  @SuppressWarnings("unchecked")
  public CreateServiceUser create(ConfigResource parent, IdString username) {
    return createServiceUserFactory.create(username.get());
  }
}

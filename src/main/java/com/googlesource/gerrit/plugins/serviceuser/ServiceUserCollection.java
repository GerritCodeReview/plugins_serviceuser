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

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.AcceptsCreate;
import com.google.gerrit.extensions.restapi.ChildCollection;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.config.ConfigResource;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ServiceUserCollection implements
    ChildCollection<ConfigResource, AccountResource>,
    AcceptsCreate<ConfigResource> {

  private final DynamicMap<RestView<AccountResource>> views;
  private final CreateServiceUser.Factory createServiceUserFactory;
  private final Provider<ListServiceUsers> list;

  @Inject
  ServiceUserCollection(DynamicMap<RestView<AccountResource>> views,
      CreateServiceUser.Factory createServiceUserFactory,
      Provider<ListServiceUsers> list) {
    this.views = views;
    this.createServiceUserFactory = createServiceUserFactory;
    this.list = list;
  }

  @Override
  public AccountResource parse(ConfigResource parent, IdString id)
      throws ResourceNotFoundException {
    throw new ResourceNotFoundException(id);
  }

  @Override
  public RestView<ConfigResource> list() {
    return list.get();
  }

  @Override
  public DynamicMap<RestView<AccountResource>> views() {
    return views;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CreateServiceUser create(ConfigResource parent, IdString username) {
    return createServiceUserFactory.create(username.get());
  }
}

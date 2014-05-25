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

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.ChildCollection;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class SshKeys implements
    ChildCollection<ServiceUserResource, ServiceUserResource.SshKey> {
  private final DynamicMap<RestView<ServiceUserResource.SshKey>> views;
  private final Provider<GetSshKeys> list;
  private final Provider<com.google.gerrit.server.account.SshKeys> sshKeys;

  @Inject
  SshKeys(DynamicMap<RestView<ServiceUserResource.SshKey>> views,
      Provider<GetSshKeys> list,
      Provider<com.google.gerrit.server.account.SshKeys> sshKeys) {
    this.views = views;
    this.list = list;
    this.sshKeys = sshKeys;
  }

  @Override
  public RestView<ServiceUserResource> list() {
    return list.get();
  }

  @Override
  public ServiceUserResource.SshKey parse(ServiceUserResource parent, IdString id)
      throws ResourceNotFoundException, OrmException {
    return new ServiceUserResource.SshKey(sshKeys.get().parse(parent.getUser(), id));
  }

  @Override
  public DynamicMap<RestView<ServiceUserResource.SshKey>> views() {
    return views;
  }
}

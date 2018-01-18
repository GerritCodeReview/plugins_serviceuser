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

import com.google.gerrit.extensions.common.SshKeyInfo;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.account.AccountResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class GetSshKey implements RestReadView<ServiceUserResource.SshKey> {
  private final Provider<com.google.gerrit.server.restapi.account.GetSshKey> getSshKey;

  @Inject
  GetSshKey(Provider<com.google.gerrit.server.restapi.account.GetSshKey> getSshKey) {
    this.getSshKey = getSshKey;
  }

  @Override
  public SshKeyInfo apply(ServiceUserResource.SshKey rsrc) {
    return getSshKey.get().apply(new AccountResource.SshKey(rsrc.getUser(), rsrc.getSshKey()));
  }
}

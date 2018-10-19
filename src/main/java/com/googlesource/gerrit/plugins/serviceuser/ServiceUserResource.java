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

import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.AccountSshKey;
import com.google.inject.TypeLiteral;

class ServiceUserResource extends AccountResource {
  static final TypeLiteral<RestView<ServiceUserResource>> SERVICE_USER_KIND =
      new TypeLiteral<RestView<ServiceUserResource>>() {};

  static final TypeLiteral<RestView<SshKey>> SERVICE_USER_SSH_KEY_KIND =
      new TypeLiteral<RestView<SshKey>>() {};

  ServiceUserResource(IdentifiedUser user) {
    super(user);
  }

  static class SshKey extends ServiceUserResource {
    private final AccountSshKey sshKey;

    SshKey(IdentifiedUser user, AccountSshKey sshKey) {
      super(user);
      this.sshKey = sshKey;
    }

    SshKey(AccountResource.SshKey sshKey) {
      super(sshKey.getUser());
      this.sshKey = sshKey.getSshKey();
    }

    AccountSshKey getSshKey() {
      return sshKey;
    }
  }
}

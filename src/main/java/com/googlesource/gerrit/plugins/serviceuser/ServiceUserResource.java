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
import com.google.gerrit.reviewdb.client.AccountSshKey;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.inject.TypeLiteral;

public class ServiceUserResource extends AccountResource {
  public static final TypeLiteral<RestView<ServiceUserResource>> SERVICE_USER_KIND =
      new TypeLiteral<RestView<ServiceUserResource>>() {};

  public static final TypeLiteral<RestView<SshKey>> SSH_KEY_KIND =
      new TypeLiteral<RestView<SshKey>>() {};

  public ServiceUserResource(IdentifiedUser user) {
    super(user);
  }

  public static class SshKey extends ServiceUserResource {
    private final AccountSshKey sshKey;

    public SshKey(IdentifiedUser user, AccountSshKey sshKey) {
      super(user);
      this.sshKey = sshKey;
    }

    public AccountSshKey getSshKey() {
      return sshKey;
    }
  }
}

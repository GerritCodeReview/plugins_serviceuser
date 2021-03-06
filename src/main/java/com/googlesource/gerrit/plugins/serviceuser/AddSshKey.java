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

import com.google.gerrit.extensions.api.accounts.SshKeyInput;
import com.google.gerrit.extensions.common.SshKeyInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;

@Singleton
class AddSshKey implements RestModifyView<ServiceUserResource, SshKeyInput> {
  private final Provider<com.google.gerrit.server.restapi.account.AddSshKey> addSshKey;

  @Inject
  AddSshKey(Provider<com.google.gerrit.server.restapi.account.AddSshKey> addSshKey) {
    this.addSshKey = addSshKey;
  }

  @Override
  public Response<SshKeyInfo> apply(ServiceUserResource rsrc, SshKeyInput input)
      throws AuthException, BadRequestException, IOException, ConfigInvalidException {
    return addSshKey.get().apply(rsrc.getUser(), input);
  }
}

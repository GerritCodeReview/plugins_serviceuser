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
import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;

import com.google.gerrit.extensions.common.Input;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;

@Singleton
class DeleteSshKey implements RestModifyView<ServiceUserResource.SshKey, Input> {
  private final Provider<GetConfig> getConfig;
  private final Provider<com.google.gerrit.server.restapi.account.DeleteSshKey> deleteSshKey;
  private final Provider<CurrentUser> self;
  private final PermissionBackend permissionBackend;

  @Inject
  DeleteSshKey(
      Provider<GetConfig> getConfig,
      Provider<com.google.gerrit.server.restapi.account.DeleteSshKey> deleteSshKey,
      Provider<CurrentUser> self,
      PermissionBackend permissionBackend) {
    this.getConfig = getConfig;
    this.deleteSshKey = deleteSshKey;
    this.self = self;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<?> apply(ServiceUserResource.SshKey rsrc, Input input)
      throws AuthException, BadRequestException, IOException, ConfigInvalidException,
          PermissionBackendException, RestApiException {

    Boolean SshAllowed;
    try {
      SshAllowed = getConfig.get().apply(new ConfigResource()).value().allowSsh;
    } catch (Exception e) {
      throw asRestApiException("Cannot get configuration", e);
    }
    if ((SshAllowed == null || !SshAllowed)) {
      permissionBackend.user(self.get()).check(ADMINISTRATE_SERVER);
    }

    return deleteSshKey
        .get()
        .apply(new AccountResource.SshKey(rsrc.getUser(), rsrc.getSshKey()), input);
  }
}

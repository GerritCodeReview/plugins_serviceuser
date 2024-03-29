// Copyright (C) 2015 The Android Open Source Project
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
import static com.google.gerrit.server.restapi.account.PutHttpPassword.generate;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.GetConfig.ConfigInfo;
import com.googlesource.gerrit.plugins.serviceuser.PutHttpPassword.Input;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;

@Singleton
public class PutHttpPassword implements RestModifyView<ServiceUserResource, Input> {
  public static class Input {
    public String httpPassword;
    public boolean generate;
  }

  private final Provider<GetConfig> getConfig;
  private final com.google.gerrit.server.restapi.account.PutHttpPassword putHttpPassword;
  private final Provider<CurrentUser> self;
  private final PermissionBackend permissionBackend;

  @Inject
  PutHttpPassword(
      Provider<GetConfig> getConfig,
      com.google.gerrit.server.restapi.account.PutHttpPassword putHttpPassword,
      Provider<CurrentUser> self,
      PermissionBackend permissionBackend) {
    this.getConfig = getConfig;
    this.putHttpPassword = putHttpPassword;
    this.self = self;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<String> apply(ServiceUserResource rsrc, Input input)
      throws ConfigInvalidException, IOException, PermissionBackendException, RestApiException,
          RuntimeException {
    if (input == null) {
      input = new Input();
    }
    input.httpPassword = Strings.emptyToNull(input.httpPassword);

    ConfigInfo config;
    try {
      config = getConfig.get().apply(new ConfigResource()).value();
    } catch (Exception e) {
      throw asRestApiException("Cannot get configuration", e);
    }

    if ((config.allowHttpPassword == null || !config.allowHttpPassword)) {
      permissionBackend.user(self.get()).check(ADMINISTRATE_SERVER);
    } else if (!input.generate && input.httpPassword != null) {
      if ((config.allowCustomHttpPassword == null || !config.allowCustomHttpPassword)) {
        permissionBackend.user(self.get()).check(ADMINISTRATE_SERVER);
      }
    }

    String newPassword = input.generate ? generate() : input.httpPassword;
    return putHttpPassword.apply(rsrc.getUser(), newPassword);
  }
}

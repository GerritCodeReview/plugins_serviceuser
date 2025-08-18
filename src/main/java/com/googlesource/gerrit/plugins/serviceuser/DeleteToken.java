// Copyright (C) 2025 The Android Open Source Project
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

import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.common.Input;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.InvalidAuthTokenException;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.email.ServiceUserOutgoingEmail;
import com.googlesource.gerrit.plugins.serviceuser.email.ServiceUserUpdatedEmailDecorator.Operation;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;

@Singleton
public class DeleteToken implements RestModifyView<ServiceUserResource.Token, Input> {
  private final PluginConfig config;
  private final com.google.gerrit.server.restapi.account.DeleteToken deleteToken;
  private final Provider<CurrentUser> self;
  private final PermissionBackend permissionBackend;
  private final ServiceUserOutgoingEmail.Factory outgoingEmailFactory;

  @Inject
  DeleteToken(
      @PluginName String pluginName,
      PluginConfigFactory pluginConfigFactory,
      com.google.gerrit.server.restapi.account.DeleteToken deleteToken,
      Provider<CurrentUser> self,
      PermissionBackend permissionBackend,
      ServiceUserOutgoingEmail.Factory outgoingEmailFactory) {
    this.config = pluginConfigFactory.getFromGerritConfig(pluginName);
    this.deleteToken = deleteToken;
    this.self = self;
    this.permissionBackend = permissionBackend;
    this.outgoingEmailFactory = outgoingEmailFactory;
  }

  @Override
  public Response<String> apply(ServiceUserResource.Token rsrc, Input input)
      throws ConfigInvalidException,
          IOException,
          PermissionBackendException,
          RestApiException,
          RuntimeException,
          InvalidAuthTokenException {
    if (!config.getBoolean("allowHttpPassword", false)) {
      permissionBackend.user(self.get()).check(ADMINISTRATE_SERVER);
    }

    Response<String> resp = deleteToken.apply(rsrc.getUser(), rsrc.getToken().id(), false);
    if (resp.statusCode() == Response.none().statusCode()) {
      outgoingEmailFactory.create(rsrc, Operation.DELETE_TOKEN).send();
    }
    return resp;
  }
}

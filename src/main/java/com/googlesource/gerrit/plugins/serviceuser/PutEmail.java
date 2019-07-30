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

import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;

import com.google.common.base.Strings;
import com.google.gerrit.exceptions.EmailException;
import com.google.gerrit.extensions.api.accounts.EmailInput;
import com.google.gerrit.extensions.restapi.DefaultInput;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.restapi.account.CreateEmail;
import com.google.gerrit.server.restapi.account.DeleteEmail;
import com.google.gerrit.server.restapi.account.PutPreferred;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.PutEmail.Input;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;

@Singleton
class PutEmail implements RestModifyView<ServiceUserResource, Input> {
  public static class Input {
    @DefaultInput public String email;
  }

  private final Provider<GetConfig> getConfig;
  private final Provider<GetEmail> getEmail;
  private final Provider<CreateEmail> createEmail;
  private final Provider<DeleteEmail> deleteEmail;
  private final Provider<PutPreferred> putPreferred;
  private final Provider<CurrentUser> self;
  private final PermissionBackend permissionBackend;

  @Inject
  PutEmail(
      Provider<GetConfig> getConfig,
      Provider<GetEmail> getEmail,
      Provider<CreateEmail> createEmail,
      Provider<DeleteEmail> deleteEmail,
      Provider<PutPreferred> putPreferred,
      Provider<CurrentUser> self,
      PermissionBackend permissionBackend) {
    this.getConfig = getConfig;
    this.getEmail = getEmail;
    this.createEmail = createEmail;
    this.deleteEmail = deleteEmail;
    this.putPreferred = putPreferred;
    this.self = self;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<?> apply(ServiceUserResource rsrc, Input input)
      throws ConfigInvalidException, EmailException, IOException,
          PermissionBackendException, RestApiException {
    Boolean emailAllowed = getConfig.get().apply(new ConfigResource()).allowEmail;
    if ((emailAllowed == null || !emailAllowed)) {
      permissionBackend.user(self.get()).check(ADMINISTRATE_SERVER);
    }

    String email = getEmail.get().apply(rsrc);
    if (Strings.emptyToNull(input.email) == null) {
      if (Strings.emptyToNull(email) == null) {
        return Response.none();
      }
      return deleteEmail.get().apply(rsrc.getUser(), email);
    } else if (email != null && email.equals(input.email)) {
      return Response.ok(email);
    } else {
      if (email != null) {
        deleteEmail.get().apply(rsrc.getUser(), email);
      }
      EmailInput in = new EmailInput();
      in.email = input.email;
      in.noConfirmation = true;
      createEmail.get().apply(rsrc.getUser(), IdString.fromDecoded(email), in);
      putPreferred.get().apply(rsrc.getUser(), input.email);
      return Response.ok(input.email);
    }
  }
}

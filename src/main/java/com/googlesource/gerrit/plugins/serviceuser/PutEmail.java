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

import com.google.common.base.Strings;
import com.google.gerrit.common.errors.EmailException;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.DefaultInput;
import com.google.gerrit.extensions.api.accounts.EmailInput;
import com.google.gerrit.extensions.restapi.MethodNotAllowedException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.CreateEmail;
import com.google.gerrit.server.account.DeleteEmail;
import com.google.gerrit.server.account.PutPreferred;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.serviceuser.PutEmail.Input;

import org.eclipse.jgit.errors.ConfigInvalidException;

import java.io.IOException;

@Singleton
class PutEmail implements RestModifyView<ServiceUserResource, Input> {
  public static class Input {
    @DefaultInput
    public String email;
  }

  private final Provider<GetConfig> getConfig;
  private final Provider<GetEmail> getEmail;
  private final Provider<CreateEmail.Factory> createEmailFactory;
  private final Provider<DeleteEmail> deleteEmail;
  private final Provider<PutPreferred> putPreferred;
  private final Provider<CurrentUser> self;

  @Inject
  PutEmail(Provider<GetConfig> getConfig,
      Provider<GetEmail> getEmail,
      Provider<CreateEmail.Factory> createEmailFactory,
      Provider<DeleteEmail> deleteEmail,
      Provider<PutPreferred> putPreferred,
      Provider<CurrentUser> self) {
    this.getConfig = getConfig;
    this.getEmail = getEmail;
    this.createEmailFactory = createEmailFactory;
    this.deleteEmail = deleteEmail;
    this.putPreferred = putPreferred;
    this.self = self;
  }

  @Override
  public Response<?> apply(ServiceUserResource rsrc, Input input)
      throws AuthException, ResourceNotFoundException,
      ResourceConflictException, MethodNotAllowedException, OrmException,
      BadRequestException, ConfigInvalidException, EmailException, IOException {
    Boolean emailAllowed = getConfig.get().apply(new ConfigResource()).allowEmail;
    if ((emailAllowed == null || !emailAllowed)
        && !self.get().getCapabilities().canAdministrateServer()) {
      throw new ResourceConflictException("setting email not allowed");
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
      createEmailFactory.get().create(input.email).apply(rsrc.getUser(), in);
      putPreferred.get().apply(rsrc.getUser(), input.email);
      return Response.ok(input.email);
    }
  }
}

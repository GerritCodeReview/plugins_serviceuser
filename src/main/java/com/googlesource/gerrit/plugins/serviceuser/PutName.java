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

import com.google.gerrit.extensions.common.NameInput;
import com.google.gerrit.extensions.restapi.MethodNotAllowedException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.email.ServiceUserOutgoingEmail;
import com.googlesource.gerrit.plugins.serviceuser.email.ServiceUserUpdatedEmailDecorator.Operation;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;

@Singleton
class PutName implements RestModifyView<ServiceUserResource, NameInput> {
  private Provider<com.google.gerrit.server.restapi.account.PutName> putName;
  private final ServiceUserOutgoingEmail.Factory outgoingEmailFactory;

  @Inject
  PutName(
      Provider<com.google.gerrit.server.restapi.account.PutName> putName,
      ServiceUserOutgoingEmail.Factory outgoingEmailFactory) {
    this.putName = putName;
    this.outgoingEmailFactory = outgoingEmailFactory;
  }

  @Override
  public Response<String> apply(ServiceUserResource rsrc, NameInput input)
      throws MethodNotAllowedException,
          ResourceNotFoundException,
          IOException,
          ConfigInvalidException {
    Response<String> resp = putName.get().apply(rsrc.getUser(), input);
    if (resp.statusCode() == Response.none().statusCode()) {
      outgoingEmailFactory.create(rsrc, Operation.DELETE_NAME).send();
    } else if (resp.statusCode() == Response.ok().statusCode()) {
      outgoingEmailFactory.create(rsrc, Operation.UPDATE_NAME).send();
    }
    return resp;
  }
}

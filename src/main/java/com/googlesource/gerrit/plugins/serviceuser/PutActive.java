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

import com.google.gerrit.extensions.common.Input;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;

@Singleton
class PutActive implements RestModifyView<ServiceUserResource, Input> {
  private final Provider<com.google.gerrit.server.restapi.account.PutActive> putActive;

  @Inject
  PutActive(Provider<com.google.gerrit.server.restapi.account.PutActive> putActive) {
    this.putActive = putActive;
  }

  @Override
  public Response<String> apply(ServiceUserResource rsrc, Input input)
      throws OrmException, IOException, ConfigInvalidException, RestApiException {
    return putActive.get().apply(rsrc, input);
  }
}

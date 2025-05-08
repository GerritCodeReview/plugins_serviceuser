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

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.ChildCollection;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.server.account.AuthToken;
import com.google.gerrit.server.account.AuthTokenAccessor;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;

@Singleton
class AuthTokens implements ChildCollection<ServiceUserResource, ServiceUserResource.Token> {
  private final DynamicMap<RestView<ServiceUserResource.Token>> views;
  private final Provider<GetTokens> list;
  private final AuthTokenAccessor tokenAccessor;

  @Inject
  AuthTokens(
      DynamicMap<RestView<ServiceUserResource.Token>> views,
      Provider<GetTokens> list,
      AuthTokenAccessor tokenAccessor) {
    this.views = views;
    this.list = list;
    this.tokenAccessor = tokenAccessor;
  }

  @Override
  public RestView<ServiceUserResource> list() {
    return list.get();
  }

  @Override
  public ServiceUserResource.Token parse(ServiceUserResource parent, IdString id)
      throws ResourceNotFoundException, IOException, ConfigInvalidException {
    Optional<AuthToken> token = tokenAccessor.getToken(parent.getUser().getAccountId(), id.get());
    if (token.isEmpty()) {
      throw new ResourceNotFoundException(id);
    }
    return new ServiceUserResource.Token(parent.getUser(), token.get());
  }

  @Override
  public DynamicMap<RestView<ServiceUserResource.Token>> views() {
    return views;
  }
}

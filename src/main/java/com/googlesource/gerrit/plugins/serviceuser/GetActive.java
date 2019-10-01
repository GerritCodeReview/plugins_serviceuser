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

import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class GetActive implements RestReadView<ServiceUserResource> {
  private final Provider<com.google.gerrit.server.restapi.account.GetActive> getActive;

  @Inject
  GetActive(Provider<com.google.gerrit.server.restapi.account.GetActive> getActive) {
    this.getActive = getActive;
  }

  @Override
  public Response<Object> apply(ServiceUserResource rsrc) {
    return Response.ok(getActive.get().apply(rsrc));
  }
}

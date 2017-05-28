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

import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.serviceuser.PutHttpPassword.Input;

import org.apache.commons.codec.binary.Base64;

import org.eclipse.jgit.errors.ConfigInvalidException;

import java.io.IOException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Singleton
public class PutHttpPassword implements RestModifyView<ServiceUserResource, Input> {
  public static class Input {
    public String httpPassword;
    public boolean generate;
  }

  private static final int LEN = 31;
  private static final SecureRandom rng;

  static {
    try {
      rng = SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Cannot create RNG for password generator", e);
    }
  }

  private final Provider<GetConfig> getConfig;
  private final com.google.gerrit.server.account.PutHttpPassword putHttpPassword;
  private final Provider<CurrentUser> self;
  private final PermissionBackend permissionBackend;

  @Inject
  PutHttpPassword(Provider<GetConfig> getConfig,
      com.google.gerrit.server.account.PutHttpPassword putHttpPassword,
      Provider<CurrentUser> self, PermissionBackend permissionBackend) {
    this.getConfig = getConfig;
    this.putHttpPassword = putHttpPassword;
    this.self = self;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<String> apply(ServiceUserResource rsrc, Input input)
      throws AuthException, ResourceConflictException, ConfigInvalidException,
      ResourceNotFoundException, OrmException, IOException, PermissionBackendException {
    if (input == null) {
      input = new Input();
    }
    input.httpPassword = Strings.emptyToNull(input.httpPassword);

    Boolean httpPasswordAllowed = getConfig.get().apply(new ConfigResource()).allowHttpPassword;
    if (input.generate || input.httpPassword == null) {
      if ((httpPasswordAllowed == null || !httpPasswordAllowed)) {
        permissionBackend.user(self).check(ADMINISTRATE_SERVER);
      }
    } else {
      permissionBackend.user(self).check(ADMINISTRATE_SERVER);
    }

    String newPassword = input.generate ? generate() : input.httpPassword;
    return putHttpPassword.apply(rsrc.getUser(), newPassword);
  }

  private static String generate() {
    byte[] rand = new byte[LEN];
    rng.nextBytes(rand);

    byte[] enc = Base64.encodeBase64(rand, false);
    StringBuilder r = new StringBuilder(enc.length);
    for (int i = 0; i < enc.length; i++) {
      if (enc[i] == '=') {
        break;
      }
      r.append((char) enc[i]);
    }
    return r.toString();
  }
}

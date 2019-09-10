// Copyright (C) 2019 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class SshKeyValidatorTest {

  private final String[] VALID_PUBLIC_KEYS = {
    "---- BEGIN SSH2 PUBLIC KEY ----\n"
        + "   Comment: comment\n"
        + "   AAAAB3NzaC1\n"
        + "   ---- END SSH2 PUBLIC KEY ----",
    "---- BEGIN PUBLIC KEY ----\n"
        + "   Comment: comment\n"
        + "   AAAAB3NzaC1\n"
        + "   ---- END PUBLIC KEY ----",
    "-----BEGIN RSA PUBLIC KEY-----\nMIIBC\n-----END RSA PUBLIC KEY-----",
    "ssh-rsa AAAAB3NzaC1",
    "ssh-dss AAAAB3NzaC1",
    "ssh-ed25519 AAAAB3NzaC1",
    "ecdsa-sha2-nistp256 AAAAB3NzaC1"
  };

  private final String[] INVALID_PUBLIC_KEYS = {
    "---- BEGIN SSH2 PUBLIC KEY ----\n   Comment: comment\n   AAAAB3NzaC1\n",
    "-----BEGIN PRIVATE KEY-----\nMIIBC\n-----END PRIVATE KEY-----",
    "AAAAB3NzaC1\n   ---- END SSH2 PUBLIC KEY ----",
    "",
    "invalid key"
  };

  @Test
  public void testValidateSshKeyFormat_Valid() {
    for (String keyToTest : VALID_PUBLIC_KEYS) {
      assertThat(SshKeyValidator.validateFormat(keyToTest)).isTrue();
    }
  }

  @Test
  public void testValidateSshKeyFormat_InValid() {
    for (String keyToTest : INVALID_PUBLIC_KEYS) {
      assertThat(SshKeyValidator.validateFormat(keyToTest)).isFalse();
    }
  }
}

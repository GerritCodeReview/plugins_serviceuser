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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ValidateSshKey {

  private static final String OPENSSH_KEY_PREFIXES[] = {
    "ssh-ed25519", "ssh-rsa", "ssh-dss", "ecdsa-sha2-"
  };
  private static final Pattern RFC_KEY_FORMAT_PATTERN =
      Pattern.compile(
          "(?s)^-{4,5}\\s?BEGIN.* PUBLIC KEY\\s?-{4,5}.+-{4,5}\\s?END.* PUBLIC KEY\\s?-{4,5}$");

  static boolean validateSshKeyFormat(String sshKey) {
    if (validateRfcSshKey(sshKey)) {
      return true;
    }

    return validateOpenSshPrefix(sshKey);
  }

  private static boolean validateOpenSshPrefix(String sshKey) {
    for (String prefix : OPENSSH_KEY_PREFIXES) {
      if (sshKey.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }

  private static boolean validateRfcSshKey(String sshKey) {
    Matcher matcher = RFC_KEY_FORMAT_PATTERN.matcher(sshKey);
    return matcher.find();
  }
}

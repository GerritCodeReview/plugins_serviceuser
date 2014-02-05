// Copyright (C) 2013 The Android Open Source Project
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

import com.google.gerrit.server.account.CreateAccount;
import com.google.gerrit.server.config.PluginConfig;

import java.util.Arrays;

public class ServiceUserInput extends CreateAccount.Input {

  public ServiceUserInput(String username, String email, String sshKey,
      PluginConfig cfg) {
    this.username = username;
    this.name = username;
    this.email = email;
    this.sshKey = sshKey;
    this.groups = Arrays.asList(cfg.getStringList("group"));
  }
}

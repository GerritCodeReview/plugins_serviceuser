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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

@RequiresCapability(CreateServiceUserCapability.ID)
@CommandMetaData(name = "create", description = "Create Service User")
class CreateServiceUserCommand extends SshCommand {

  @Argument(index = 0, required = true, metaVar = "USERNAME", usage = "name of the service user")
  private String username;

  @Option(
      name = "--ssh-key",
      required = true,
      metaVar = "-|KEY",
      usage = "public key for SSH authentication")
  private String sshKey;

  @Inject private CreateServiceUser createServiceUser;

  @Override
  protected void run()
      throws OrmException, IOException, UnloggedFailure, ConfigInvalidException,
          PermissionBackendException {
    CreateServiceUser.Input input = new CreateServiceUser.Input();
    input.sshKey = readSshKey();
    input.username = username;

    try {
      createServiceUser.apply(new ConfigResource(), IdString.fromDecoded(username), input);
    } catch (RestApiException e) {
      throw die(e.getMessage());
    }
  }

  private String readSshKey() throws IOException {
    if (sshKey == null) {
      return null;
    }
    if ("-".equals(sshKey)) {
      StringBuilder key = new StringBuilder();
      BufferedReader br = new BufferedReader(new InputStreamReader(in, UTF_8));
      String line;
      while ((line = br.readLine()) != null) {
        key.append(line).append("\n");
      }
      sshKey = key.toString();
    }
    return sshKey;
  }
}

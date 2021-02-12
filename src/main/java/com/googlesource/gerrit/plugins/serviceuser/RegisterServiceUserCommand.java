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

import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.sshd.CommandMetaData;
import com.google.gerrit.sshd.SshCommand;
import com.google.inject.Inject;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

@RequiresCapability(CreateServiceUserCapability.ID)
@CommandMetaData(name = "register", description = "Register Service User")
class RegisterServiceUserCommand extends SshCommand {

  @Argument(index = 0, required = true, metaVar = "USERNAME", usage = "name of the service user")
  private String username;

  @Option(
      name = "--creator",
      required = false,
      metaVar = "CREATOR",
      usage = "name of the creator of the service user")
  private String creator;

  @Option(
      name = "--owner",
      required = false,
      metaVar = "OWNER",
      usage = "group that owns the service user")
  private String owner;

  @Inject private RegisterServiceUser registerServiceUser;

  @Override
  protected void run()
      throws IOException, UnloggedFailure, ConfigInvalidException, PermissionBackendException {
    RegisterServiceUser.Input input = new RegisterServiceUser.Input();
    input.username = username;
    input.creator = creator;
    input.owner = owner;

    try {
      registerServiceUser.apply(new ConfigResource(), IdString.fromDecoded(username), input);
    } catch (RestApiException e) {
      throw die(e.getMessage());
    }
  }
}

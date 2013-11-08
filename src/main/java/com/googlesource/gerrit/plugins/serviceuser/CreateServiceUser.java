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

import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.account.CreateAccount;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.Input;

@RequiresCapability(CreateServiceUserCapability.ID)
public class CreateServiceUser implements RestModifyView<ConfigResource, Input> {
  static class Input {
    String username;
    String sshKey;
  }

  public static interface Factory {
    CreateServiceUser create(String username);
  }

  private final PluginConfigFactory cfg;
  private final String pluginName;
  private final CreateAccount.Factory createAccountFactory;
  private final String username;

  @Inject
  CreateServiceUser(PluginConfigFactory cfg, @PluginName String pluginName,
      CreateAccount.Factory createAccountFactory, @Assisted String username) {
    this.cfg = cfg;
    this.pluginName = pluginName;
    this.createAccountFactory = createAccountFactory;
    this.username = username;
  }

  @Override
  public Object apply(ConfigResource resource, Input input)
      throws BadRequestException, ResourceConflictException,
      UnprocessableEntityException, OrmException {
    if (input == null) {
      input = new Input();
    }
    if (input.username != null && !username.equals(input.username)) {
      throw new BadRequestException("username must match URL");
    }
    if (Strings.isNullOrEmpty(input.sshKey)) {
      throw new BadRequestException("sshKey not set");
    }

    CreateAccount.Input in =
        new ServiceUserInput(username, input.sshKey,
            cfg.getFromGerritConfig(pluginName));
    return createAccountFactory.create(username).apply(TopLevelResource.INSTANCE, in);
  }
}

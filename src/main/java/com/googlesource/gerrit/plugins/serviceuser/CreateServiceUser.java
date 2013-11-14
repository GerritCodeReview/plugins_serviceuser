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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.server.account.CreateAccount;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.Input;

import java.util.Arrays;
import java.util.List;

@RequiresCapability(CreateServiceUserCapability.ID)
public class CreateServiceUser implements RestModifyView<ConfigResource, Input> {
  static class Input {
    String username;
    String sshKey;
  }

  public static interface Factory {
    CreateServiceUser create(String username);
  }

  private final PluginConfig cfg;
  private final CreateAccount.Factory createAccountFactory;
  private final String username;
  private final List<String> blockedNames;

  @Inject
  CreateServiceUser(PluginConfigFactory cfgFactory, @PluginName String pluginName,
      CreateAccount.Factory createAccountFactory, @Assisted String username) {
    this.cfg = cfgFactory.getFromGerritConfig(pluginName);
    this.createAccountFactory = createAccountFactory;
    this.username = username;
    this.blockedNames =
        Lists.transform(Arrays.asList(cfg.getStringList("block")),
            new Function<String, String>() {
              @Override
              public String apply(String blockedName) {
                return blockedName.toLowerCase();
              }
            });
  }

  @Override
  public Object apply(ConfigResource resource, Input input)
      throws BadRequestException, ResourceConflictException,
      UnprocessableEntityException, OrmException {
    if (input == null) {
      input = new Input();
    }
    if (!username.equals(input.username)) {
      throw new BadRequestException("username must match URL");
    }
    if (Strings.isNullOrEmpty(input.sshKey)) {
      throw new BadRequestException("sshKey not set");
    }

    if (blockedNames.contains(username.toLowerCase())) {
      throw new BadRequestException("The username '" + username
          + "' is not allowed as name for service users.");
    }

    CreateAccount.Input in =
        new ServiceUserInput(username, input.sshKey, cfg);
    return createAccountFactory.create(username).apply(TopLevelResource.INSTANCE, in);
  }
}

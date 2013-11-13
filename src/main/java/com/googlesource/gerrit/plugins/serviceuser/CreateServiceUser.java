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
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.account.CreateAccount;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.Input;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@RequiresCapability(CreateServiceUserCapability.ID)
public class CreateServiceUser implements RestModifyView<ConfigResource, Input> {
  private static final String USER_SECTION = "user";
  private static final String CREATED_BY_KEY = "createdBy";
  private static final String CREATED_AT_KEY = "createdAt";

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
  private final Provider<CurrentUser> userProvider;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final Project.NameKey allProjects;
  private final ProjectLevelConfig storage;
  private final DateFormat rfc2822DateFormatter;

  @Inject
  CreateServiceUser(PluginConfigFactory cfgFactory,
      @PluginName String pluginName,
      CreateAccount.Factory createAccountFactory,
      Provider<CurrentUser> userProvider,
      @GerritPersonIdent PersonIdent gerritIdent,
      MetaDataUpdate.User metaDataUpdateFactory,
      ProjectCache projectCache,
      @Assisted String username) {
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
    this.userProvider = userProvider;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.storage = projectCache.getAllProjects().getConfig(pluginName + ".db");
    this.allProjects = projectCache.getAllProjects().getProject().getNameKey();
    this.rfc2822DateFormatter =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    this.rfc2822DateFormatter.setCalendar(Calendar.getInstance(
        gerritIdent.getTimeZone(), Locale.US));
  }

  @Override
  public Object apply(ConfigResource resource, Input input)
      throws BadRequestException, ResourceConflictException,
      UnprocessableEntityException, OrmException, IOException {
    if (input == null) {
      input = new Input();
    }
    if (input.username != null && !username.equals(input.username)) {
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
    Object response = createAccountFactory.create(username)
            .apply(TopLevelResource.INSTANCE, in);

    Config db = storage.get();
    db.setString(USER_SECTION, username, CREATED_BY_KEY,
        userProvider.get().getUserName());
    db.setString(USER_SECTION, username, CREATED_AT_KEY,
        rfc2822DateFormatter.format(new Date()));

    MetaDataUpdate md = metaDataUpdateFactory.create(allProjects);
    md.setMessage("Create Service User '" + username + "'\n");
    storage.commit(md);

    return response;
  }
}

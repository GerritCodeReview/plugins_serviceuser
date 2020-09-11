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

import static com.google.gerrit.server.api.ApiUtil.asRestApiException;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.api.accounts.AccountInput;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestCollectionCreateView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountLoader;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.meta.MetaDataUpdate;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectLevelConfig;
import com.google.gerrit.server.restapi.account.CreateAccount;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.Input;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.PersonIdent;

@RequiresCapability(CreateServiceUserCapability.ID)
@Singleton
class CreateServiceUser
    implements RestCollectionCreateView<ConfigResource, ServiceUserResource, Input> {
  public static final String USER = "user";
  public static final String KEY_CREATED_BY = "createdBy";
  public static final String KEY_CREATED_AT = "createdAt";
  public static final String KEY_CREATOR_ID = "creatorId";
  public static final String KEY_OWNER = "owner";

  static class Input {
    String username;
    String sshKey;
    String email;
  }

  private final PluginConfig cfg;
  private final CreateAccount createAccount;
  private final List<String> blockedNames;
  private final Provider<CurrentUser> userProvider;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final Project.NameKey allProjects;
  private final ProjectLevelConfig.Bare storage;
  private final DateFormat rfc2822DateFormatter;
  private final Provider<GetConfig> getConfig;
  private final AccountLoader.Factory accountLoader;

  @Inject
  CreateServiceUser(
      PluginConfigFactory cfgFactory,
      @PluginName String pluginName,
      CreateAccount createAccount,
      Provider<CurrentUser> userProvider,
      @GerritPersonIdent PersonIdent gerritIdent,
      MetaDataUpdate.User metaDataUpdateFactory,
      ProjectCache projectCache,
      Provider<GetConfig> getConfig,
      AccountLoader.Factory accountLoader) {
    this.cfg = cfgFactory.getFromGerritConfig(pluginName);
    this.createAccount = createAccount;
    this.blockedNames =
        Lists.transform(
            Arrays.asList(cfg.getStringList("block")),
            new Function<String, String>() {
              @Override
              public String apply(String blockedName) {
                return blockedName.toLowerCase();
              }
            });
    this.userProvider = userProvider;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.storage = new ProjectLevelConfig.Bare(pluginName + ".db");
    this.allProjects = projectCache.getAllProjects().getProject().getNameKey();
    this.rfc2822DateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    this.rfc2822DateFormatter.setCalendar(
        Calendar.getInstance(gerritIdent.getTimeZone(), Locale.US));
    this.getConfig = getConfig;
    this.accountLoader = accountLoader;
  }

  @Override
  public Response<ServiceUserInfo> apply(
      ConfigResource parentResource, IdString id, CreateServiceUser.Input input)
      throws RestApiException, IOException, ConfigInvalidException, PermissionBackendException {
    CurrentUser user = userProvider.get();
    if (user == null || !user.isIdentifiedUser()) {
      throw new AuthException("authentication required");
    }

    if (input == null) {
      input = new Input();
    }
    String username = id.get();
    if (input.username != null && !username.equals(input.username)) {
      throw new BadRequestException("username must match URL");
    }

    if (Strings.isNullOrEmpty(input.sshKey)) {
      throw new BadRequestException("sshKey not set");
    }

    if (!SshKeyValidator.validateFormat(input.sshKey)) {
      throw new BadRequestException("sshKey invalid.");
    }

    if (blockedNames.contains(username.toLowerCase())) {
      throw new BadRequestException(
          "The username '" + username + "' is not allowed as name for service users.");
    }

    input.email = Strings.emptyToNull(input.email);
    if (input.email != null) {
      Boolean emailAllowed;
      try {
        emailAllowed = getConfig.get().apply(new ConfigResource()).value().allowEmail;
      } catch (Exception e) {
        throw asRestApiException("Cannot get configuration", e);
      }
      if (emailAllowed == null || !emailAllowed) {
        throw new ResourceConflictException("email not allowed");
      }
    }

    AccountInput in = new ServiceUserInput(username, input.email, input.sshKey);
    in.groups = Arrays.asList(cfg.getStringList("group"));

    AccountInfo response;
    try {
      response = createAccount.apply(IdString.fromDecoded(username), in).value();
    } catch (Exception e) {
      throw asRestApiException("Cannot create account", e);
    }

    String creator = user.getUserName().get();
    Account.Id creatorId = ((IdentifiedUser) user).getAccountId();
    String creationDate = rfc2822DateFormatter.format(new Date());

    Config db = storage.getConfig();
    db.setInt(USER, username, KEY_CREATOR_ID, creatorId.get());
    if (creator != null) {
      db.setString(USER, username, KEY_CREATED_BY, creator);
    }
    db.setString(USER, username, KEY_CREATED_AT, creationDate);

    MetaDataUpdate md = metaDataUpdateFactory.create(allProjects);
    md.setMessage("Create service user '" + username + "'\n");
    storage.commit(md);
    ServiceUserInfo info = new ServiceUserInfo(response);
    AccountLoader al = accountLoader.create(true);
    info.createdBy = al.get(creatorId);
    al.fill();
    info.createdAt = creationDate;
    return Response.created(info);
  }
}

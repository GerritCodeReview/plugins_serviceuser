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
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.client.AccountGroupMember;
import com.google.gerrit.reviewdb.client.AccountGroupMemberAudit;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountInfo;
import com.google.gerrit.server.account.CreateAccount;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.util.TimeUtil;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

import com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.Input;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@RequiresCapability(CreateServiceUserCapability.ID)
public class CreateServiceUser implements RestModifyView<ConfigResource, Input> {
  private static final Logger log =
      LoggerFactory.getLogger(CreateServiceUser.class);

  public static final String USER = "user";
  public static final String KEY_CREATED_BY = "createdBy";
  public static final String KEY_CREATED_AT = "createdAt";
  public static final String KEY_OWNER = "owner";

  static class Input {
    String username;
    String sshKey;
    String email;
  }

  public static interface Factory {
    CreateServiceUser create(String username);
  }

  private final PluginConfig cfg;
  private final CreateAccount.Factory createAccountFactory;
  private final GroupCache groupCache;
  private final AccountCache accountCache;
  private final Provider<IdentifiedUser> currentUser;
  private final Provider<ReviewDb> db;
  private final String username;
  private final List<String> blockedNames;
  private final Provider<CurrentUser> userProvider;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final Project.NameKey allProjects;
  private final ProjectLevelConfig storage;
  private final DateFormat rfc2822DateFormatter;
  private final Provider<GetConfig> getConfig;

  @Inject
  CreateServiceUser(PluginConfigFactory cfgFactory,
      @PluginName String pluginName,
      CreateAccount.Factory createAccountFactory,
      GroupCache groupCache,
      AccountCache accountCache,
      Provider<IdentifiedUser> currentUser,
      Provider<ReviewDb> db,
      Provider<CurrentUser> userProvider,
      @GerritPersonIdent PersonIdent gerritIdent,
      MetaDataUpdate.User metaDataUpdateFactory,
      ProjectCache projectCache,
      @Assisted String username,
      Provider<GetConfig> getConfig) {
    this.cfg = cfgFactory.getFromGerritConfig(pluginName);
    this.createAccountFactory = createAccountFactory;
    this.groupCache = groupCache;
    this.accountCache = accountCache;
    this.currentUser = currentUser;
    this.db = db;
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
    this.getConfig = getConfig;
  }

  @Override
  public Response<ServiceUserInfo> apply(ConfigResource resource, Input input)
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

    input.email = Strings.emptyToNull(input.email);
    if (input.email != null) {
      Boolean emailAllowed = getConfig.get().apply(new ConfigResource()).allowEmail;
      if (emailAllowed == null || !emailAllowed) {
        throw new ResourceConflictException("email not allowed");
      }
    }

    CreateAccount.Input in =
        new ServiceUserInput(username, input.email, input.sshKey);
    Response<AccountInfo> response =
        createAccountFactory.create(username).apply(TopLevelResource.INSTANCE, in);
    addToGroups(response.value()._id, cfg.getStringList("group"));

    String creator = userProvider.get().getUserName();
    String creationDate = rfc2822DateFormatter.format(new Date());

    Config db = storage.get();
    db.setString(USER, username, KEY_CREATED_BY, creator);
    db.setString(USER, username, KEY_CREATED_AT, creationDate);

    MetaDataUpdate md = metaDataUpdateFactory.create(allProjects);
    md.setMessage("Create service user '" + username + "'\n");
    storage.commit(md);

    ServiceUserInfo info = new ServiceUserInfo(response.value());
    info.createdBy = creator;
    info.createdAt = creationDate;
    return Response.created(info);
  }

  private void addToGroups(Account.Id accountId, String[] groupNames)
      throws OrmException {
    for (String groupName : groupNames) {
      AccountGroup group = groupCache.get(new AccountGroup.NameKey(groupName));
      if (group != null) {
        AccountGroupMember m =
            new AccountGroupMember(new AccountGroupMember.Key(
                accountId, group.getId()));
        db.get().accountGroupMembersAudit().insert(Collections.singleton(
            new AccountGroupMemberAudit(
                m, currentUser.get().getAccountId(), TimeUtil.nowTs())));
        db.get().accountGroupMembers().insert(Collections.singleton(m));
      } else {
        log.error(String.format(
            "Could not add new service user %s to group %s: group not found",
            username, groupName));
      }
    }
    if (groupNames.length > 0) {
      accountCache.evict(accountId);
    }
  }
}

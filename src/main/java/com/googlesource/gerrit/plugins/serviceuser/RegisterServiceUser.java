// Copyright (C) 2021 The Android Open Source Project
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

import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;

import com.google.common.base.Strings;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestCollectionCreateView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountLoader;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.account.AccountResolver.UnresolvableAccountException;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.git.meta.MetaDataUpdate;
import com.google.gerrit.server.group.GroupResolver;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectLevelConfig;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;
import com.googlesource.gerrit.plugins.serviceuser.RegisterServiceUser.Input;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.PersonIdent;

@RequiresCapability(CreateServiceUserCapability.ID)
@Singleton
class RegisterServiceUser
    implements RestCollectionCreateView<ConfigResource, ServiceUserResource, Input> {
  public static final String USER = "user";
  public static final String KEY_CREATED_BY = "createdBy";
  public static final String KEY_CREATED_AT = "createdAt";
  public static final String KEY_CREATOR_ID = "creatorId";
  public static final String KEY_OWNER = "owner";

  static class Input {
    String username;
    String creator;
    String owner;
  }

  private final AccountResolver accountResolver;
  private final GroupResolver groupResolver;
  private final PluginConfig cfg;
  private final Provider<CurrentUser> userProvider;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final Project.NameKey allProjects;
  private final ProjectLevelConfig storage;
  private final DateFormat rfc2822DateFormatter;
  private final AccountLoader.Factory accountLoader;
  private final PermissionBackend permissionBackend;
  private final BlockedNameFilter blockedNameFilter;

  @Inject
  RegisterServiceUser(
      AccountResolver accountResolver,
      GroupResolver groupResolver,
      PluginConfigFactory cfgFactory,
      @PluginName String pluginName,
      Provider<CurrentUser> userProvider,
      @GerritPersonIdent PersonIdent gerritIdent,
      MetaDataUpdate.User metaDataUpdateFactory,
      ProjectCache projectCache,
      AccountLoader.Factory accountLoader,
      PermissionBackend permissionBackend,
      BlockedNameFilter blockedNameFilter) {
    this.accountResolver = accountResolver;
    this.groupResolver = groupResolver;
    this.cfg = cfgFactory.getFromGerritConfig(pluginName);
    this.userProvider = userProvider;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.storage = projectCache.getAllProjects().getConfig(pluginName + ".db");
    this.allProjects = projectCache.getAllProjects().getProject().getNameKey();
    this.rfc2822DateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    this.rfc2822DateFormatter.setCalendar(
        Calendar.getInstance(gerritIdent.getTimeZone(), Locale.US));
    this.accountLoader = accountLoader;
    this.permissionBackend = permissionBackend;
    this.blockedNameFilter = blockedNameFilter;
  }

  @Override
  public Response<ServiceUserInfo> apply(ConfigResource parentResource, IdString id, Input input)
      throws RestApiException, IOException, ConfigInvalidException, PermissionBackendException {
    CurrentUser requestingUser = userProvider.get();
    if (requestingUser == null || !requestingUser.isIdentifiedUser()) {
      throw new AuthException("authentication required");
    }

    if (input == null) {
      input = new Input();
    }

    IdentifiedUser user;
    try {
      user = accountResolver.resolve(input.username).asUniqueUser();
    } catch (UnresolvableAccountException e) {
      throw new BadRequestException("Username does not exist");
    }

    if (!requestingUser
            .asIdentifiedUser()
            .getAccountId()
            .toString()
            .equals(user.getAccountId().toString())
        && !permissionBackend.user(requestingUser).testOrFalse(ADMINISTRATE_SERVER)) {
      throw new BadRequestException(
          "Requesting user is not administrator or the user to be registered as service user.");
    }

    Config db = storage.get();
    if (db.getSubsections(USER).contains(input.username)) {
      throw new BadRequestException("The user already is a serviceuser.");
    }

    if (blockedNameFilter.apply(input.username.toLowerCase())) {
      throw new BadRequestException(
          "The username '" + input.username + "' is not allowed as name for service users.");
    }

    String creator;
    Account.Id creatorId;
    if (Strings.isNullOrEmpty(input.creator)) {
      creator = requestingUser.getUserName().get();
      creatorId = ((IdentifiedUser) requestingUser).getAccountId();
    } else {
      creator = input.creator;
      creatorId = accountResolver.resolve(input.creator).asUniqueUser().getAccountId();
    }
    String creationDate = rfc2822DateFormatter.format(new Date());

    String owner = "";
    if (!Strings.isNullOrEmpty(input.owner)) {
      try {
        owner = groupResolver.parse(input.owner).getGroupUUID().toString();
      } catch (UnresolvableAccountException e) {
        throw new BadRequestException("The group '" + input.owner + "' does not exist");
      }
    }

    db.setInt(USER, input.username, KEY_CREATOR_ID, creatorId.get());
    if (creator != null) {
      db.setString(USER, input.username, KEY_CREATED_BY, creator);
    }
    if (!Strings.isNullOrEmpty(owner)) {
      db.setString(USER, input.username, KEY_OWNER, owner);
    }
    db.setString(USER, input.username, KEY_CREATED_AT, creationDate);

    MetaDataUpdate md = metaDataUpdateFactory.create(allProjects);
    md.setMessage("Create service user '" + input.username + "'\n");
    storage.commit(md);

    ServiceUserInfo info = new ServiceUserInfo(new AccountInfo(user.getAccountId().get()));
    AccountLoader al = accountLoader.create(true);
    info.createdBy = al.get(creatorId);
    al.fill();
    info.createdAt = creationDate;
    return Response.created(info);
  }
}

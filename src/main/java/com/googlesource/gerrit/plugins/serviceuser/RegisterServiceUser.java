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
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_CREATED_AT;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_CREATED_BY;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_CREATOR_ID;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_OWNER;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;

import com.google.common.base.Strings;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.MethodNotAllowedException;
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

  static class Input {
    String username;
    String creator;
    String owner;
  }

  private final Provider<ProjectLevelConfig.Bare> configProvider;
  private final AccountResolver accountResolver;
  private final GroupResolver groupResolver;
  private final Provider<CurrentUser> userProvider;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final Project.NameKey allProjects;
  private final DateFormat rfc2822DateFormatter;
  private final AccountLoader.Factory accountLoader;
  private final StorageCache storageCache;
  private final PermissionBackend permissionBackend;
  private final BlockedNameFilter blockedNameFilter;

  @Inject
  RegisterServiceUser(
      Provider<ProjectLevelConfig.Bare> configProvider,
      AccountResolver accountResolver,
      GroupResolver groupResolver,
      Provider<CurrentUser> userProvider,
      @GerritPersonIdent PersonIdent gerritIdent,
      MetaDataUpdate.User metaDataUpdateFactory,
      ProjectCache projectCache,
      AccountLoader.Factory accountLoader,
      StorageCache storageCache,
      PermissionBackend permissionBackend,
      BlockedNameFilter blockedNameFilter) {
    this.configProvider = configProvider;
    this.accountResolver = accountResolver;
    this.groupResolver = groupResolver;
    this.userProvider = userProvider;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.allProjects = projectCache.getAllProjects().getProject().getNameKey();
    this.rfc2822DateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    this.rfc2822DateFormatter.setCalendar(
        Calendar.getInstance(gerritIdent.getTimeZone(), Locale.US));
    this.accountLoader = accountLoader;
    this.storageCache = storageCache;
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

    if (!requestingUser.getAccountId().equals(user.getAccountId())
        && !permissionBackend.user(requestingUser).testOrFalse(ADMINISTRATE_SERVER)) {
      throw new MethodNotAllowedException("Forbidden");
    }

    if (blockedNameFilter.isBlocked(input.username)) {
      throw new BadRequestException(
          "The username '" + input.username + "' is not allowed as name for service users.");
    }

    String creator;
    Account.Id creatorId;
    if (Strings.isNullOrEmpty(input.creator)) {
      creator = requestingUser.getUserName().orElse(null);
      creatorId = requestingUser.asIdentifiedUser().getAccountId();
    } else {
      creator = input.creator;
      creatorId = accountResolver.resolve(input.creator).asUniqueUser().getAccountId();
    }
    String creationDate = rfc2822DateFormatter.format(new Date());

    String owner = null;
    if (!Strings.isNullOrEmpty(input.owner)) {
      try {
        owner = groupResolver.parse(input.owner).getGroupUUID().toString();
      } catch (UnresolvableAccountException e) {
        throw new BadRequestException("The group '" + input.owner + "' does not exist");
      }
    }

    try (MetaDataUpdate md = metaDataUpdateFactory.create(allProjects)) {
      ProjectLevelConfig.Bare update = configProvider.get();
      update.load(md);

      Config db = update.getConfig();
      if (db.getSubsections(USER).contains(input.username)) {
        return Response.none();
      }
      db.setInt(USER, input.username, KEY_CREATOR_ID, creatorId.get());
      if (creator != null) {
        db.setString(USER, input.username, KEY_CREATED_BY, creator);
      }
      if (owner != null) {
        db.setString(USER, input.username, KEY_OWNER, owner);
      }
      db.setString(USER, input.username, KEY_CREATED_AT, creationDate);

      md.setMessage("Create service user '" + input.username + "'\n");
      update.commit(md);
      storageCache.invalidate();
    }

    ServiceUserInfo info = new ServiceUserInfo(new AccountInfo(user.getAccountId().get()));
    AccountLoader al = accountLoader.create(true);
    info.createdBy = al.get(creatorId);
    al.fill();
    info.createdAt = creationDate;
    return Response.created(info);
  }
}

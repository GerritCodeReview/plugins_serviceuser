// Copyright (C) 2025 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.serviceuser.email;

import static com.google.gerrit.server.mail.EmailFactories.AUTH_TOKEN_WILL_EXPIRE;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.KEY_OWNER;
import static com.googlesource.gerrit.plugins.serviceuser.CreateServiceUser.USER;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.GroupDescription;
import com.google.gerrit.exceptions.EmailException;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.lifecycle.LifecycleModule;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.account.AuthToken;
import com.google.gerrit.server.account.AuthTokenAccessor;
import com.google.gerrit.server.account.GroupControl;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.ScheduleConfig;
import com.google.gerrit.server.config.ScheduleConfig.Schedule;
import com.google.gerrit.server.git.WorkQueue;
import com.google.gerrit.server.group.GroupResolver;
import com.google.gerrit.server.group.GroupResource;
import com.google.gerrit.server.mail.EmailFactories;
import com.google.gerrit.server.restapi.group.ListMembers;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.StorageCache;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;

@Singleton
public class ServiceUserAuthTokenExpiryNotifier implements Runnable {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final long FIRST_NOTIFICATION_BEFORE_EXPIRY = 7L; // 7 days

  private final StorageCache storageCache;
  private final AuthTokenAccessor tokenAccessor;
  private final EmailFactories emailFactories;
  private final AccountCache accountCache;
  private final Provider<ListMembers> listMembers;
  private final OneOffRequestContext oneOffRequestContext;
  private final GroupControl.Factory groupControlFactory;
  private final GroupResolver groupResolver;
  private final String canonicalWebUrl;

  public static Module module() {
    return new LifecycleModule() {
      @Override
      protected void configure() {
        bind(ServiceUserAuthTokenExpiryNotifier.class);
        listener().to(ServiceUserAuthTokenExpiryNotifier.Lifecycle.class);
      }
    };
  }

  static class Lifecycle implements LifecycleListener {
    private final WorkQueue queue;
    private final ServiceUserAuthTokenExpiryNotifier notifier;
    private final Optional<Schedule> schedule;

    @Inject
    Lifecycle(WorkQueue queue, ServiceUserAuthTokenExpiryNotifier notifier) {
      this.queue = queue;
      this.notifier = notifier;
      schedule = ScheduleConfig.Schedule.create(TimeUnit.DAYS.toMillis(1), "00:00");
    }

    @Override
    public void start() {
      if (schedule.isPresent()) {
        queue.scheduleAtFixedRate(notifier, schedule.get());
      }
    }

    @Override
    public void stop() {
      // handled by WorkQueue.stop() already
    }
  }

  @Inject
  public ServiceUserAuthTokenExpiryNotifier(
      StorageCache storageCache,
      AuthTokenAccessor tokenAccessor,
      EmailFactories emailFactories,
      AccountCache accountCache,
      Provider<ListMembers> listMembers,
      OneOffRequestContext oneOffRequestContext,
      GroupControl.Factory groupControlFactory,
      GroupResolver groupResolver,
      @CanonicalWebUrl String canonicalWebUrl) {
    this.storageCache = storageCache;
    this.tokenAccessor = tokenAccessor;
    this.emailFactories = emailFactories;
    this.accountCache = accountCache;
    this.listMembers = listMembers;
    this.oneOffRequestContext = oneOffRequestContext;
    this.groupControlFactory = groupControlFactory;
    this.groupResolver = groupResolver;
    this.canonicalWebUrl = canonicalWebUrl;
  }

  @Override
  public void run() {
    Instant now = Instant.now();
    try {
      Config db = storageCache.get();
      for (String username : db.getSubsections(USER)) {
        Optional<AccountState> optAccount = accountCache.getByUsername(username);
        Set<Account.Id> owners = resolveOwners(username, db);
        if (optAccount.isPresent()) {
          Account account = optAccount.get().account();
          for (AuthToken token : tokenAccessor.getTokens(account.id())) {
            if (token.expirationDate().isEmpty()) {
              continue;
            }
            Instant expirationDate = token.expirationDate().get();
            if (expirationDate.isBefore(now.plus(FIRST_NOTIFICATION_BEFORE_EXPIRY, ChronoUnit.DAYS))
                && expirationDate.isAfter(
                    now.plus(FIRST_NOTIFICATION_BEFORE_EXPIRY - 1, ChronoUnit.DAYS))) {
              logger.atInfo().log(
                  "Token %s for account %s is expiring soon.", token.id(), account.id());
              String authTokenSettingsUrl =
                  String.format("%sx/serviceuser/user/%d", canonicalWebUrl, account.id().get());
              emailFactories
                  .createOutgoingEmail(
                      AUTH_TOKEN_WILL_EXPIRE,
                      emailFactories.createAuthTokenWillExpireEmail(
                          account, token, owners, authTokenSettingsUrl))
                  .send();
            }
          }
        }
      }
    } catch (IOException | ConfigInvalidException e) {
      throw new RuntimeException("Failed to read accounts from NoteDB", e);
    } catch (EmailException e) {
      logger.atSevere().withCause(e).log("Failed to send token expiry notification email");
    }
  }

  private Set<Account.Id> resolveOwners(String username, Config db) throws EmailException {
    Set<Account.Id> owners = new HashSet<>();

    String ownerGroup = db.getString(USER, username, KEY_OWNER);

    if (ownerGroup != null) {
      try (ManualRequestContext ctx = oneOffRequestContext.open()) {
        GroupDescription.Basic group = groupResolver.parseId(ownerGroup);
        GroupControl ctl = groupControlFactory.controlFor(group);
        ListMembers lm = listMembers.get();
        GroupResource rsrc = new GroupResource(ctl);
        lm.setRecursive(true);
        try {
          for (AccountInfo a : lm.apply(rsrc).value()) {
            owners.add(Account.id(a._accountId));
          }
        } catch (Exception e) {
          throw new EmailException(
              "Could not compute receipients for serviceuser update notice.", e);
        }
      }
    }

    return owners;
  }
}

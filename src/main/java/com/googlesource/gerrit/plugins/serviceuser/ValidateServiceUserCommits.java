// Copyright (C) 2014 The Android Open Source Project
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

import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.PersonIdent;

@Singleton
class ValidateServiceUserCommits implements CommitValidationListener {
  private final ServiceUserResolver serviceUserResolver;
  private final AccountCache accountCache;

  @Inject
  ValidateServiceUserCommits(ServiceUserResolver serviceUserResolver, AccountCache accountCache) {
    this.serviceUserResolver = serviceUserResolver;
    this.accountCache = accountCache;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    try {
      PersonIdent committer = receiveEvent.commit.getCommitterIdent();
      ServiceUserInfo serviceUser = serviceUserResolver.getAsServiceUser(committer);
      if (serviceUser != null) {
        if (serviceUser.owner != null
            && serviceUserResolver.listActiveOwners(serviceUser).isEmpty()) {
          throw new CommitValidationException(
              String.format(
                  "Commit %s of service user %s (%s) is rejected because "
                      + "all service user owner accounts are inactive.",
                  receiveEvent.commit.getId().getName(),
                  committer.getName(),
                  committer.getEmailAddress()));
        }
        Optional<AccountState> creator =
            accountCache.get(Account.id(serviceUser.createdBy._accountId));
        if (!creator.isPresent() || !creator.get().account().isActive()) {
          throw new CommitValidationException(
              String.format(
                  "Commit %s of service user %s (%s) is rejected because "
                      + "the account of the service creator is inactive.",
                  receiveEvent.commit.getId().getName(),
                  committer.getName(),
                  committer.getEmailAddress()));
        }
      }
    } catch (RestApiException e) {
      throw new CommitValidationException(
          "Internal error while checking for service user commits.", e);
    }
    return Collections.emptyList();
  }
}

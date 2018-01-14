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

import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.account.GroupMembership;
import com.google.gerrit.server.group.ListMembers;
import com.google.gerrit.server.util.RequestContext;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.SchemaFactory;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.PersonIdent;

@Singleton
class ServiceUserResolver {
  private final AccountResolver resolver;
  private final IdentifiedUser.GenericFactory genericUserFactory;
  private final Provider<GetServiceUser> getServiceUser;
  private final Provider<ListMembers> listMembers;
  private final SchemaFactory<ReviewDb> schema;
  private final ThreadLocalRequestContext tl;
  private final AccountCache accountCache;

  @Inject
  ServiceUserResolver(
      AccountResolver resolver,
      IdentifiedUser.GenericFactory genericUserFactory,
      Provider<GetServiceUser> getServiceUser,
      Provider<ListMembers> listMembers,
      SchemaFactory<ReviewDb> schema,
      ThreadLocalRequestContext tl,
      AccountCache accountCache) {
    this.resolver = resolver;
    this.genericUserFactory = genericUserFactory;
    this.getServiceUser = getServiceUser;
    this.listMembers = listMembers;
    this.schema = schema;
    this.tl = tl;
    this.accountCache = accountCache;
  }

  ServiceUserInfo getAsServiceUser(PersonIdent committerIdent)
      throws ConfigInvalidException, IOException, OrmException {
    StringBuilder committer = new StringBuilder();
    committer.append(committerIdent.getName());
    committer.append(" <");
    committer.append(committerIdent.getEmailAddress());
    committer.append("> ");

    Account account = resolver.find(committer.toString());
    if (account == null) {
      return null;
    }
    try {
      return getServiceUser
          .get()
          .apply(new ServiceUserResource(genericUserFactory.create(account.getId())));
    } catch (ResourceNotFoundException e) {
      return null;
    }
  }

  List<AccountInfo> listOwners(ServiceUserInfo serviceUser) throws OrmException {
    if (serviceUser.owner == null) {
      return Collections.emptyList();
    }

    try (ReviewDb db = schema.open()) {
      RequestContext context =
          new RequestContext() {
            @Override
            public CurrentUser getUser() {
              return new CurrentUser() {

                @Override
                public GroupMembership getEffectiveGroups() {
                  return new GroupMembership() {
                    @Override
                    public Set<AccountGroup.UUID> intersection(
                        Iterable<AccountGroup.UUID> groupIds) {
                      return null;
                    }

                    @Override
                    public Set<AccountGroup.UUID> getKnownGroups() {
                      return null;
                    }

                    @Override
                    public boolean containsAnyOf(Iterable<AccountGroup.UUID> groupIds) {
                      return true;
                    }

                    @Override
                    public boolean contains(AccountGroup.UUID groupId) {
                      return true;
                    }
                  };
                }
              };
            }

            @Override
            public Provider<ReviewDb> getReviewDbProvider() {
              return new Provider<ReviewDb>() {
                @Override
                public ReviewDb get() {
                  return db;
                }
              };
            }
          };
      RequestContext old = tl.setContext(context);
      try {
        ListMembers lm = listMembers.get();
        lm.setRecursive(true);
        List<AccountInfo> owners = new ArrayList<>();
        for (AccountInfo a : lm.apply(new AccountGroup.UUID(serviceUser.owner.id))) {
          owners.add(a);
        }
        return owners;
      } finally {
        tl.setContext(old);
      }
    }
  }

  List<AccountInfo> listActiveOwners(ServiceUserInfo serviceUser) throws OrmException {
    List<AccountInfo> activeOwners = new ArrayList<>();
    for (AccountInfo owner : listOwners(serviceUser)) {
      AccountState accountState = accountCache.get(new Account.Id(owner._accountId));
      if (accountState != null && accountState.getAccount().isActive()) {
        activeOwners.add(owner);
      }
    }
    return activeOwners;
  }
}

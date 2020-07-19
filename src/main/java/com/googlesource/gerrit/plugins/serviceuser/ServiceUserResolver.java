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

import static com.google.gerrit.server.api.ApiUtil.asRestApiException;

import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.AccountGroup;
import com.google.gerrit.entities.GroupDescription;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountResolver;
import com.google.gerrit.server.account.AccountResolver.UnresolvableAccountException;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.account.GroupControl;
import com.google.gerrit.server.account.GroupMembership;
import com.google.gerrit.server.group.GroupResolver;
import com.google.gerrit.server.group.GroupResource;
import com.google.gerrit.server.restapi.group.ListMembers;
import com.google.gerrit.server.util.RequestContext;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.lib.PersonIdent;

@Singleton
class ServiceUserResolver {
  private final AccountResolver resolver;
  private final IdentifiedUser.GenericFactory genericUserFactory;
  private final Provider<GetServiceUser> getServiceUser;
  private final Provider<ListMembers> listMembers;
  private final ThreadLocalRequestContext tl;
  private final AccountCache accountCache;
  private final GroupControl.Factory groupControlFactory;
  private final GroupResolver groupResolver;

  @Inject
  ServiceUserResolver(
      AccountResolver resolver,
      IdentifiedUser.GenericFactory genericUserFactory,
      Provider<GetServiceUser> getServiceUser,
      Provider<ListMembers> listMembers,
      ThreadLocalRequestContext tl,
      AccountCache accountCache,
      GroupControl.Factory groupControlFactory,
      GroupResolver groupResolver) {
    this.resolver = resolver;
    this.genericUserFactory = genericUserFactory;
    this.getServiceUser = getServiceUser;
    this.listMembers = listMembers;
    this.tl = tl;
    this.accountCache = accountCache;
    this.groupControlFactory = groupControlFactory;
    this.groupResolver = groupResolver;
  }

  ServiceUserInfo getAsServiceUser(PersonIdent committerIdent) throws RestApiException {
    StringBuilder committer = new StringBuilder();
    committer.append(committerIdent.getName());
    committer.append(" <");
    committer.append(committerIdent.getEmailAddress());
    committer.append("> ");

    try {
      Account account = resolver.resolve(committer.toString()).asUnique().account();
      return getServiceUser
          .get()
          .apply(new ServiceUserResource(genericUserFactory.create(account.id())))
          .value();
    } catch (ResourceNotFoundException | UnresolvableAccountException e) {
      return null;
    } catch (Exception e) {
      throw asRestApiException("Cannot get service user", e);
    }
  }

  List<AccountInfo> listOwners(ServiceUserInfo serviceUser)
      throws RestApiException, RuntimeException {
    if (serviceUser.owner == null) {
      return Collections.emptyList();
    }

    RequestContext context =
        new RequestContext() {
          @Override
          public CurrentUser getUser() {
            return new CurrentUser() {

              @Override
              public GroupMembership getEffectiveGroups() {
                return new GroupMembership() {
                  @Override
                  public Set<AccountGroup.UUID> intersection(Iterable<AccountGroup.UUID> groupIds) {
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

              @Override
              public Object getCacheKey() {
                return null;
              }
            };
          }
        };
    RequestContext old = tl.setContext(context);
    try {
      GroupDescription.Basic group = groupResolver.parseId(serviceUser.owner.id);
      GroupControl ctl = groupControlFactory.controlFor(group);
      ListMembers lm = listMembers.get();
      GroupResource rsrc = new GroupResource(ctl);
      lm.setRecursive(true);
      List<AccountInfo> owners = new ArrayList<>();
      try {
        for (AccountInfo a : lm.apply(rsrc).value()) {
          owners.add(a);
        }
      } catch (Exception e) {
        throw asRestApiException("Cannot list group members", e);
      }
      return owners;
    } finally {
      tl.setContext(old);
    }
  }

  List<AccountInfo> listActiveOwners(ServiceUserInfo serviceUser)
      throws RestApiException, RuntimeException {
    List<AccountInfo> activeOwners = new ArrayList<>();
    for (AccountInfo owner : listOwners(serviceUser)) {
      Optional<AccountState> accountState = accountCache.get(Account.id(owner._accountId));
      if (accountState.isPresent() && accountState.get().account().isActive()) {
        activeOwners.add(owner);
      }
    }
    return activeOwners;
  }
}

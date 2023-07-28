// Copyright (C) 2023 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.GroupInfo;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;
import java.util.Collections;
import java.util.Optional;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValidateServiceUserCommitsTest {
  private static final Account.Id SERVICE_USER_ACCOUNT_ID = Account.id(100);
  private static final Account.Id CREATOR_ACCOUNT_ID = Account.id(200);
  private static final Account.Id OWNER_ACCOUNT_ID = Account.id(300);

  @Mock ServiceUserResolver resolver;
  @Mock AccountCache accountCache;

  TestRepository<?> testRepo;
  CommitReceivedEvent event = new CommitReceivedEvent();
  AccountInfo serviceUserAccountInfo = new AccountInfo(SERVICE_USER_ACCOUNT_ID.get());
  PersonIdent serviceUserIdent = new PersonIdent("robot", "robot@example.com");

  ValidateServiceUserCommits validator;

  @Before
  public void setUp() throws Exception {
    testRepo = new TestRepository<>(new InMemoryRepository(new DfsRepositoryDescription("repo")));
    event.commit = testRepo.commit().committer(serviceUserIdent).create();
    validator = new ValidateServiceUserCommits(resolver, accountCache);
  }

  @Test
  public void committerNotServiceUser_pass() throws Exception {
    when(resolver.getAsServiceUser(serviceUserIdent)).thenReturn(null);
    assertThat(validator.onCommitReceived(event)).isEmpty();
  }

  @Test
  public void noCreatorAndNoActiveOwners_reject() throws Exception {
    ServiceUserInfo serviceUser = setupWithNonExistingCreator();
    ownerGroupNotSet(serviceUser);
    assertThrows(CommitValidationException.class, () -> validator.onCommitReceived(event));

    ownerGroupSetButContainsNoActiveMembers(serviceUser);
    assertThrows(CommitValidationException.class, () -> validator.onCommitReceived(event));
  }

  @Test
  public void inactiveCreatorAndNoActiveOwners_reject() throws Exception {
    ServiceUserInfo serviceUser = setupWithInactiveCreator();
    ownerGroupNotSet(serviceUser);
    assertThrows(CommitValidationException.class, () -> validator.onCommitReceived(event));

    ownerGroupSetButContainsNoActiveMembers(serviceUser);
    assertThrows(CommitValidationException.class, () -> validator.onCommitReceived(event));
  }

  @Test
  public void activeCreatorAnNoActiveOwners_pass() throws Exception {
    ServiceUserInfo serviceUser = setupWithActiveCreator();
    ownerGroupNotSet(serviceUser);
    assertThat(validator.onCommitReceived(event)).isEmpty();

    ownerGroupSetButContainsNoActiveMembers(serviceUser);
    assertThat(validator.onCommitReceived(event)).isEmpty();
  }

  @Test
  public void inactiveCreatorAndActiveOwners_pass() throws Exception {
    ServiceUserInfo serviceUser = setupWithInactiveCreator();
    ownerGroupSetAndContainsActiveMembers(serviceUser);
    assertThat(validator.onCommitReceived(event)).isEmpty();
  }

  @Test
  public void activeCreatorAndActiveOwners_pass() throws Exception {
    ServiceUserInfo serviceUser = setupWithActiveCreator();
    ownerGroupSetAndContainsActiveMembers(serviceUser);
    assertThat(validator.onCommitReceived(event)).isEmpty();
  }

  private ServiceUserInfo setupWithNonExistingCreator() throws Exception {
    ServiceUserInfo serviceUser = new ServiceUserInfo(serviceUserAccountInfo);
    when(resolver.getAsServiceUser(serviceUserIdent)).thenReturn(serviceUser);

    serviceUser.createdBy = new AccountInfo(CREATOR_ACCOUNT_ID.get());
    // accountCache returns empty Optional: means creator account not found
    when(accountCache.get(CREATOR_ACCOUNT_ID)).thenReturn(Optional.empty());

    return serviceUser;
  }

  private ServiceUserInfo setupWithActiveCreator() throws Exception {
    return setupWithCreator(true);
  }

  private ServiceUserInfo setupWithInactiveCreator() throws Exception {
    return setupWithCreator(false);
  }

  private ServiceUserInfo setupWithCreator(boolean active) throws Exception {
    ServiceUserInfo serviceUser = new ServiceUserInfo(serviceUserAccountInfo);
    when(resolver.getAsServiceUser(serviceUserIdent)).thenReturn(serviceUser);

    serviceUser.createdBy = new AccountInfo(CREATOR_ACCOUNT_ID.get());
    AccountState creatorAccountState = mock(AccountState.class);
    Account creatorAccount = mock(Account.class);
    when(creatorAccountState.account()).thenReturn(creatorAccount);
    when(creatorAccount.isActive()).thenReturn(active);
    when(accountCache.get(CREATOR_ACCOUNT_ID)).thenReturn(Optional.of(creatorAccountState));

    return serviceUser;
  }

  private void ownerGroupNotSet(ServiceUserInfo serviceUser) {
    serviceUser.owner = null;
  }

  private void ownerGroupSetButContainsNoActiveMembers(ServiceUserInfo serviceUser)
      throws Exception {
    serviceUser.owner = new GroupInfo();
    lenient().when(resolver.listActiveOwners(serviceUser)).thenReturn(Collections.emptyList());
  }

  private void ownerGroupSetAndContainsActiveMembers(ServiceUserInfo serviceUser) throws Exception {
    serviceUser.owner = new GroupInfo();
    lenient()
        .when(resolver.listActiveOwners(serviceUser))
        .thenReturn(Collections.singletonList(new AccountInfo(OWNER_ACCOUNT_ID.get())));
  }
}

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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.exceptions.EmailException;
import com.google.gerrit.extensions.common.Input;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.VersionedAuthorizedKeys;
import com.google.gerrit.server.mail.send.DeleteKeySender;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.ssh.SshKeyCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

@Singleton
class DeleteSshKey implements RestModifyView<ServiceUserResource.SshKey, Input> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final VersionedAuthorizedKeys.Accessor authorizedKeys;
  private final DeleteKeySender.Factory deleteKeySenderFactory;
  private final SshKeyCache sshKeyCache;

  @Inject
  DeleteSshKey(
      VersionedAuthorizedKeys.Accessor authorizedKeys,
      DeleteKeySender.Factory deleteKeySenderFactory,
      SshKeyCache sshKeyCache) {
    this.authorizedKeys = authorizedKeys;
    this.deleteKeySenderFactory = deleteKeySenderFactory;
    this.sshKeyCache = sshKeyCache;
  }

  @Override
  public Response<?> apply(ServiceUserResource.SshKey rsrc, Input input)
      throws AuthException, RepositoryNotFoundException, IOException, ConfigInvalidException,
          PermissionBackendException {
    IdentifiedUser user = rsrc.getUser();
    authorizedKeys.deleteKey(user.getAccountId(), rsrc.getSshKey().seq());
    try {
      deleteKeySenderFactory.create(user, rsrc.getSshKey()).send();
    } catch (EmailException e) {
      logger.atSevere().withCause(e).log(
          "Cannot send SSH key deletion message to %s", user.getAccount().preferredEmail());
    }
    user.getUserName().ifPresent(sshKeyCache::evict);

    return Response.none();
  }
}

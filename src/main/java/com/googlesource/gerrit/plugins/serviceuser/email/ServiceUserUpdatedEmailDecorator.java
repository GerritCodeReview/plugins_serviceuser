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

import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.GroupDescription;
import com.google.gerrit.exceptions.EmailException;
import com.google.gerrit.extensions.api.changes.RecipientType;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.account.GroupControl;
import com.google.gerrit.server.config.DefaultUrlFormatter;
import com.google.gerrit.server.group.GroupResolver;
import com.google.gerrit.server.group.GroupResource;
import com.google.gerrit.server.mail.send.MessageIdGenerator;
import com.google.gerrit.server.mail.send.OutgoingEmail;
import com.google.gerrit.server.mail.send.OutgoingEmail.EmailDecorator;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.restapi.group.ListMembers;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.gerrit.server.util.time.TimeUtil;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser;
import com.googlesource.gerrit.plugins.serviceuser.GetServiceUser.ServiceUserInfo;
import com.googlesource.gerrit.plugins.serviceuser.ServiceUserResource;
import java.io.IOException;

public class ServiceUserUpdatedEmailDecorator implements EmailDecorator {
  public enum Operation {
    ADD_SSH_KEY("An SSH key was added."),
    CREATE_TOKEN("An authentication token was created."),
    INACTIVATE("The service user was inactivated."),
    DELETE_SSH_KEY("An SSH key was deleted."),
    DELETE_TOKEN("An authentication token was deleted."),
    ACTIVATE("The service user was activated."),
    DELETE_EMAIL("The email address was deleted."),
    ADD_EMAIL("The email address was updated."),
    DELETE_NAME("The full name was deleted."),
    UPDATE_NAME("The full name was updated."),
    UPDATE_OWNER("The owner group was updated."),
    DELETE_OWNER("The owner group was deleted.");

    public final String description;

    Operation(String description) {
      this.description = description;
    }
  }

  public interface Factory {
    ServiceUserUpdatedEmailDecorator create(
        ServiceUserResource serviceUserResource, Operation operation);
  }

  private OutgoingEmail email;

  private final Provider<GetServiceUser> getServiceUser;
  private final Provider<ListMembers> listMembers;
  private final OneOffRequestContext oneOffRequestContext;
  private final GroupControl.Factory groupControlFactory;
  private final GroupResolver groupResolver;
  private final MessageIdGenerator messageIdGenerator;
  private final DefaultUrlFormatter urlFomatter;

  private final ServiceUserResource serviceUserResource;
  private final Operation operation;

  private ServiceUserInfo serviceUserInfo;

  @AssistedInject
  public ServiceUserUpdatedEmailDecorator(
      Provider<GetServiceUser> getServiceUser,
      Provider<ListMembers> listMembers,
      OneOffRequestContext oneOffRequestContext,
      GroupControl.Factory groupControlFactory,
      GroupResolver groupResolver,
      MessageIdGenerator messageIdGenerator,
      DefaultUrlFormatter urlFomatter,
      @Assisted ServiceUserResource serviceUserResource,
      @Assisted Operation operation) {
    this.getServiceUser = getServiceUser;
    this.listMembers = listMembers;
    this.oneOffRequestContext = oneOffRequestContext;
    this.groupControlFactory = groupControlFactory;
    this.groupResolver = groupResolver;
    this.messageIdGenerator = messageIdGenerator;
    this.urlFomatter = urlFomatter;

    this.serviceUserResource = serviceUserResource;
    this.operation = operation;
  }

  @Override
  public void init(OutgoingEmail email) throws EmailException {
    this.email = email;

    try (ManualRequestContext ctx = oneOffRequestContext.open()) {
      try {
        serviceUserInfo = getServiceUser.get().apply(serviceUserResource).value();
      } catch (IOException | RestApiException | PermissionBackendException e) {
        throw new EmailException(
            String.format(
                "Failed to get service user details for %s",
                serviceUserResource.getUser().getUserName()),
            e);
      }
    }

    this.email.setHeader(
        "Subject",
        String.format(
            "[Gerrit Code Review] Service User '%s' has been updated.", serviceUserInfo.username));
    this.email.setMessageId(
        messageIdGenerator.fromReasonAccountIdAndTimestamp(
            "Serviceuser_updated", Account.id(serviceUserInfo._accountId), TimeUtil.now()));

    if (serviceUserInfo.owner == null) {
      this.email.addByAccountId(RecipientType.TO, Account.id(serviceUserInfo.createdBy._accountId));
    } else {
      try (ManualRequestContext ctx = oneOffRequestContext.open()) {
        GroupDescription.Basic group = groupResolver.parseId(serviceUserInfo.owner.id);
        GroupControl ctl = groupControlFactory.controlFor(group);
        ListMembers lm = listMembers.get();
        GroupResource rsrc = new GroupResource(ctl);
        lm.setRecursive(true);
        try {
          for (AccountInfo a : lm.apply(rsrc).value()) {
            this.email.addByAccountId(RecipientType.TO, Account.id(a._accountId));
          }
        } catch (Exception e) {
          throw new EmailException(
              "Could not compute receipients for serviceuser update notice.", e);
        }
      }
    }
  }

  @Override
  public void populateEmailContent() throws EmailException {
    email.addSoyEmailDataParam("serviceUserName", serviceUserInfo.username);
    email.addSoyEmailDataParam("operation", operation.description);
    email.addSoyEmailDataParam(
        "serviceUserUrl",
        urlFomatter
            .getRestUrl(String.format("x/serviceuser/user/%d", serviceUserInfo._accountId))
            .get());

    email.appendText(email.textTemplate("ServiceUserUpdated"));
    if (email.useHtml()) {
      email.appendHtml(email.soyHtmlTemplate("ServiceUserUpdatedHtml"));
    }
  }
}

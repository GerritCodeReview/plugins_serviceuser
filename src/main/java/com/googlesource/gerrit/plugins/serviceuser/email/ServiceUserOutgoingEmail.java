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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.exceptions.EmailException;
import com.google.gerrit.server.mail.send.OutgoingEmailFactory;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.googlesource.gerrit.plugins.serviceuser.ServiceUserResource;
import com.googlesource.gerrit.plugins.serviceuser.email.ServiceUserUpdatedEmailDecorator.Operation;

public class ServiceUserOutgoingEmail {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ServiceUserUpdatedEmailDecorator.Factory emailDecoratorFactory;
  private final OutgoingEmailFactory outgoingEmailFactory;

  private final ServiceUserResource serviceUserResource;
  private final Operation operation;

  public interface Factory {
    ServiceUserOutgoingEmail create(ServiceUserResource serviceUserResource, Operation operation);
  }

  @AssistedInject
  public ServiceUserOutgoingEmail(
      ServiceUserUpdatedEmailDecorator.Factory emailDecoratorFactory,
      OutgoingEmailFactory outgoingEmailFactory,
      @Assisted ServiceUserResource serviceUserResource,
      @Assisted Operation operation) {
    this.emailDecoratorFactory = emailDecoratorFactory;
    this.outgoingEmailFactory = outgoingEmailFactory;

    this.serviceUserResource = serviceUserResource;
    this.operation = operation;
  }

  public void send() {
    try {
      outgoingEmailFactory
          .create(
              "ServiceUserUpdated", emailDecoratorFactory.create(serviceUserResource, operation))
          .send();
    } catch (EmailException e) {
      logger.atSevere().withCause(e).log("Failed to send email about serviceuser update");
    }
  }
}

// Copyright (C) 2013 The Android Open Source Project
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

import static com.google.gerrit.server.config.ConfigResource.CONFIG_KIND;
import static com.googlesource.gerrit.plugins.serviceuser.ServiceUserResource.SERVICE_USER_KIND;
import static com.googlesource.gerrit.plugins.serviceuser.ServiceUserResource.SSH_KEY_KIND;

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.config.CapabilityDefinition;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class Module extends AbstractModule {

  @Override
  protected void configure() {
    bind(CapabilityDefinition.class)
        .annotatedWith(Exports.named(CreateServiceUserCapability.ID))
        .to(CreateServiceUserCapability.class);
    DynamicSet.bind(binder(), TopMenu.class).to(ServiceUserMenu.class);
    install(new RestApiModule() {
      @Override
      protected void configure() {
        DynamicMap.mapOf(binder(), SERVICE_USER_KIND);
        DynamicMap.mapOf(binder(), SSH_KEY_KIND);
        bind(ServiceUserCollection.class);
        child(CONFIG_KIND, "serviceusers").to(ServiceUserCollection.class);
        get(SERVICE_USER_KIND).to(GetServiceUser.class);
        install(new FactoryModuleBuilder().build(CreateServiceUser.Factory.class));
        get(CONFIG_KIND, "config").to(GetConfig.class);
        put(CONFIG_KIND, "config").to(PutConfig.class);
        child(SERVICE_USER_KIND, "sshkeys").to(SshKeys.class);
        get(SSH_KEY_KIND).to(GetSshKey.class);
        post(SERVICE_USER_KIND, "sshkeys").to(AddSshKey.class);
        delete(SSH_KEY_KIND).to(DeleteSshKey.class);
        get(SERVICE_USER_KIND, "name").to(GetName.class);
        put(SERVICE_USER_KIND, "name").to(PutName.class);
        delete(SERVICE_USER_KIND, "name").to(PutName.class);
        get(SERVICE_USER_KIND, "email").to(GetEmail.class);
        put(SERVICE_USER_KIND, "email").to(PutEmail.class);
        delete(SERVICE_USER_KIND, "email").to(PutEmail.class);
      }
    });
  }
}

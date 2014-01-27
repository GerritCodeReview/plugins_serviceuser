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

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.config.CapabilityDefinition;
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
    DynamicSet.bind(binder(), TopMenu.class).to(CreateServiceUserMenu.class);
    install(new RestApiModule() {
      @Override
      protected void configure() {
        bind(ServiceUserCollection.class);
        child(CONFIG_KIND, "serviceusers").to(ServiceUserCollection.class);
        install(new FactoryModuleBuilder().build(CreateServiceUser.Factory.class));
        get(CONFIG_KIND, "messages").to(GetMessages.class);
        put(CONFIG_KIND, "messages").to(PutMessages.class);
      }
    });
  }
}

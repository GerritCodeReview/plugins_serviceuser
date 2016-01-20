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

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.client.MenuItem;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.CapabilityControl;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.List;

class ServiceUserMenu implements TopMenu {
  private final String pluginName;
  private final Provider<CurrentUser> userProvider;
  private final List<MenuEntry> menuEntries;
  private final Provider<ListServiceUsers> listServiceUsers;

  @Inject
  ServiceUserMenu(@PluginName String pluginName,
      Provider<CurrentUser> userProvider,
      Provider<ListServiceUsers> listServiceUsers) {
    this.pluginName = pluginName;
    this.userProvider = userProvider;
    this.listServiceUsers = listServiceUsers;
    menuEntries = Lists.newArrayList();

    List<MenuItem> peopleItems = Lists.newArrayListWithExpectedSize(2);
    if (canCreateServiceUser()) {
      peopleItems.add(new MenuItem("Create Service User", "#/x/" + pluginName + "/create", ""));
    }
    if (canCreateServiceUser() || hasServiceUser()) {
      peopleItems.add(new MenuItem("List Service Users", "#/x/" + pluginName + "/list", ""));
    }
    if (!peopleItems.isEmpty()) {
      menuEntries.add(new MenuEntry("People", peopleItems));
    }
  }

  private boolean canCreateServiceUser() {
    if (userProvider.get().isIdentifiedUser()) {
      CapabilityControl ctl = userProvider.get().getCapabilities();
      return ctl.canPerform(pluginName + "-" + CreateServiceUserCapability.ID)
          || ctl.canAdministrateServer();
    } else {
      return false;
    }
  }

  private boolean hasServiceUser() {
    try {
      return !listServiceUsers.get().apply(new ConfigResource()).isEmpty();
    } catch (AuthException | OrmException e) {
      return false;
    }
  }

  @Override
  public List<MenuEntry> getEntries() {
    return menuEntries;
  }
}

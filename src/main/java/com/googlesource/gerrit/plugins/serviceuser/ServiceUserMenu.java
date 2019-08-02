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

import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.access.PluginPermission;
import com.google.gerrit.extensions.client.MenuItem;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.List;

class ServiceUserMenu implements TopMenu {
  private final String pluginName;
  private final Provider<CurrentUser> userProvider;
  private final List<MenuEntry> menuEntries;
  private final PermissionBackend permissionBackend;

  @Inject
  ServiceUserMenu(
      @PluginName String pluginName,
      Provider<CurrentUser> userProvider,
      PermissionBackend permissionBackend) {
    this.pluginName = pluginName;
    this.userProvider = userProvider;
    menuEntries = Lists.newArrayList();
    this.permissionBackend = permissionBackend;

    List<MenuItem> menuItems = Lists.newArrayListWithExpectedSize(2);
    if (canCreateServiceUser()) {
      menuItems.add(new MenuItem("List", "#/x/" + pluginName + "/list"));
    }
    if (!menuItems.isEmpty()) {
      menuEntries.add(new MenuEntry("Service Users", menuItems));
    }
  }

  private boolean canCreateServiceUser() {
    if (userProvider.get().isIdentifiedUser()) {
      IdentifiedUser user = userProvider.get().asIdentifiedUser();
      return permissionBackend
              .user(user)
              .testOrFalse(new PluginPermission(pluginName, CreateServiceUserCapability.ID))
          || permissionBackend.user(user).testOrFalse(ADMINISTRATE_SERVER);
    }
    return false;
  }

  @Override
  public List<MenuEntry> getEntries() {
    return menuEntries;
  }
}

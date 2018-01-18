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
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.errors.ConfigInvalidException;

class ServiceUserMenu implements TopMenu {
  private final String pluginName;
  private final Provider<CurrentUser> userProvider;
  private final List<MenuEntry> menuEntries;
  private final Provider<ListServiceUsers> listServiceUsers;
  private final PermissionBackend permissionBackend;

  @Inject
  ServiceUserMenu(
      @PluginName String pluginName,
      Provider<CurrentUser> userProvider,
      Provider<ListServiceUsers> listServiceUsers,
      PermissionBackend permissionBackend)
      throws IOException, PermissionBackendException, ConfigInvalidException, BadRequestException {
    this.pluginName = pluginName;
    this.userProvider = userProvider;
    this.listServiceUsers = listServiceUsers;
    menuEntries = Lists.newArrayList();
    this.permissionBackend = permissionBackend;

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
      return permissionBackend
              .user(userProvider)
              .testOrFalse(new PluginPermission(pluginName, CreateServiceUserCapability.ID))
          || permissionBackend.user(userProvider).testOrFalse(ADMINISTRATE_SERVER);
    }
    return false;
  }

  private boolean hasServiceUser()
      throws PermissionBackendException, IOException, ConfigInvalidException, BadRequestException {
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

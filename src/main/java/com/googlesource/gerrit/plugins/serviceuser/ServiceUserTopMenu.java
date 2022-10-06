// Copyright (C) 2022 The Android Open Source Project
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
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.api.access.PluginPermission;
import com.google.gerrit.extensions.client.MenuItem;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.List;

@RequiresCapability("printHello")
public class ServiceUserTopMenu implements TopMenu {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final String pluginName;
  private final Provider<CurrentUser> userProvider;
  private final List<MenuEntry> menuEntries;
  private final PermissionBackend permissionBackend;

  @Inject
  public ServiceUserTopMenu(
      @PluginName String pluginName,
      Provider<CurrentUser> userProvider,
      PermissionBackend permissionBackend) {
    this.pluginName = pluginName;
    this.userProvider = userProvider;
    this.permissionBackend = permissionBackend;
    menuEntries = new ArrayList<>();

    try {
      if (canSeeMenuEntry()) {
        menuEntries.add(
            new MenuEntry(
                "Browse",
                Lists.newArrayList(new MenuItem("Service Users", "/x/serviceuser/list"))));
      }
    } catch (PermissionBackendException e) {
      logger.atSevere().withCause(e).log("Unable to compute permissions.");
    }
  }

  private boolean canSeeMenuEntry() throws PermissionBackendException {
    if (userProvider.get().isIdentifiedUser()) {
      return permissionBackend
          .currentUser()
          .test(new PluginPermission(pluginName, CreateServiceUserCapability.ID));
    }
    return false;
  }

  @Override
  public List<MenuEntry> getEntries() {
    return menuEntries;
  }
}

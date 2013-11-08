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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gerrit.extensions.webui.TopMenu;

public class CreateServiceUserMenu implements TopMenu {
  public final static String MENU_ID = "serviceuser_create-service-user";
  private final List<MenuEntry> menuEntries;

  public CreateServiceUserMenu() {
    menuEntries = new ArrayList<TopMenu.MenuEntry>();
    menuEntries.add(new MenuEntry("People", Collections
        .singletonList(new MenuItem("Create Service User", "", "", MENU_ID))));
  }

  @Override
  public List<MenuEntry> getEntries() {
    return menuEntries;
  }
}
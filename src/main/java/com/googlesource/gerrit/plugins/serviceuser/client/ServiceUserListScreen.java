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

package com.googlesource.gerrit.plugins.serviceuser.client;

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.rpc.NativeMap;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ServiceUserListScreen extends VerticalPanel {
  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Service Users");
      screen.show(new ServiceUserListScreen());
    }
  }

  ServiceUserListScreen() {
    setStyleName("serviceuser-panel");

    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "serviceusers")
        .get(NativeMap.copyKeysIntoChildren("username",
            new AsyncCallback<NativeMap<ServiceUserInfo>>() {
              @Override
              public void onSuccess(NativeMap<ServiceUserInfo> info) {
                display(info);
              }

              @Override
              public void onFailure(Throwable caught) {
                // never invoked
              }
            }));
  }

  private void display(NativeMap<ServiceUserInfo> info) {
    int columns = 5;
    FlexTable t = new FlexTable();
    t.setStyleName("serviceuser-serviceUserTable");
    FlexCellFormatter fmt = t.getFlexCellFormatter();
    for (int c = 0; c < columns; c++) {
      fmt.addStyleName(0, c, "dataHeader");
      fmt.addStyleName(0, c, "topMostCell");
    }
    fmt.addStyleName(0, 0, "leftMostCell");

    t.setText(0, 0, "Username");
    t.setText(0, 1, "Full Name");
    t.setText(0, 2, "Email");
    t.setText(0, 3, "Created By");
    t.setText(0, 4, "Created At");

    int row = 1;
    for (String username : info.keySet()) {
      ServiceUserInfo a = info.get(username);

      for (int c = 0; c < columns; c++) {
        fmt.addStyleName(row, c, "dataCell");
        fmt.addStyleName(row, 0, "leftMostCell");
      }

      t.setWidget(row, 0, new InlineHyperlink(
          username, "/x/" + Plugin.get().getName() + "/user/" + username));
      t.setText(row, 1, a.name());
      t.setText(row, 2, a.email());
      t.setText(row, 3, a.created_by());
      t.setText(row, 4, a.created_at());
      row++;
    }

    add(t);
  }
}

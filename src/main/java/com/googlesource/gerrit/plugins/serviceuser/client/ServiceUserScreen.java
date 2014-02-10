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
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ServiceUserScreen extends VerticalPanel {
  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Service User " + screen.getToken(1));
      screen.show(new ServiceUserScreen(screen.getToken(1)));
    }
  }

  ServiceUserScreen(String serviceUser) {
    setStyleName("serviceuser-panel");

    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "serviceusers")
        .id(serviceUser).get(new AsyncCallback<ServiceUserInfo>() {
            @Override
            public void onSuccess(ServiceUserInfo info) {
              display(info);
            }

            @Override
            public void onFailure(Throwable caught) {
              // never invoked
            }
          });
  }

  private void display(ServiceUserInfo info) {
    MyTable t = new MyTable();
    t.setStyleName("serviceuser-serviceUserInfoTable");
    t.addRow("Username", info.username());
    t.addRow("Full Name", info.name());
    t.addRow("Email Address", info.email());
    t.addRow("Created By", info.created_by());
    t.addRow("Created At", info.created_at());
    add(t);

    add(new SshPanel(info.username()));
  }

  private static class MyTable extends FlexTable {
    private static int row = 0;

    private void addRow(String label, String value) {
      setWidget(row, 0, new Label(label + ":"));
      setWidget(row, 1, new Label(value));
      row++;
    }
  }
}

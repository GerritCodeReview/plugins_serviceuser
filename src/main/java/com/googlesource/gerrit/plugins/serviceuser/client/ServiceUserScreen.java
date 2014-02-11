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
import com.google.gerrit.plugin.client.rpc.NativeString;
import com.google.gerrit.plugin.client.rpc.NoContent;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

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
            public void onSuccess(final ServiceUserInfo serviceUserInfo) {
              new RestApi("config").id("server")
                  .view(Plugin.get().getPluginName(), "config")
                  .get(new AsyncCallback<ConfigInfo>() {
                    @Override
                    public void onSuccess(ConfigInfo configInfo) {
                      display(serviceUserInfo, configInfo.getAllowEmail());
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                      // never invoked
                    }
                  });
            }

            @Override
            public void onFailure(Throwable caught) {
              // never invoked
            }
          });
  }

  private void display(ServiceUserInfo info, boolean allowEmail) {
    MyTable t = new MyTable();
    t.setStyleName("serviceuser-serviceUserInfoTable");
    t.addRow("Account State", createActiveToggle(info.username()));
    t.addRow("Username", info.username());
    t.addRow("Full Name", new EditableValue(info.username(), info.name()) {
      @Override
      protected void save(String serviceUser, final String newValue) {
        new RestApi("config").id("server")
            .view(Plugin.get().getPluginName(), "serviceusers").id(serviceUser)
            .view("name").put(newValue, new AsyncCallback<NativeString>() {
              @Override
              public void onSuccess(NativeString result) {
                updateValue(newValue);
              }

              @Override
              public void onFailure(Throwable caught) {
                // never invoked
              }
            });
      }
    });
    if (allowEmail) {
      t.addRow("Email Address", new EditableValue(info.username(), info.email()) {
        @Override
        protected void save(String serviceUser, final String newValue) {
          new RestApi("config").id("server")
              .view(Plugin.get().getPluginName(), "serviceusers").id(serviceUser)
              .view("email").put(newValue, new AsyncCallback<NativeString>() {
                @Override
                public void onSuccess(NativeString result) {
                  updateValue(newValue);
                }

                @Override
                public void onFailure(Throwable caught) {
                  // never invoked
                }
              });
        }
      });
    } else {
      t.addRow("Email Address", info.email());
    }
    t.addRow("Created By", info.created_by());
    t.addRow("Created At", info.created_at());
    add(t);

    add(new SshPanel(info.username()));
  }

  private ToggleButton createActiveToggle(final String serviceUser) {
    final ToggleButton activeToggle = new ToggleButton();
    activeToggle.setStyleName("serviceuser-toggleButton");
    activeToggle.setVisible(false);
    activeToggle.setValue(true);
    activeToggle.setText("Active");
    activeToggle.setValue(false);
    activeToggle.setText("Inactive");

    new RestApi("config").id("server")
        .view(Plugin.get().getPluginName(), "serviceusers").id(serviceUser)
        .view("active").get(NativeString.unwrap(new AsyncCallback<String>() {
      @Override
      public void onSuccess(String result) {
        activeToggle.setValue(result != null && "ok".equals(result.trim()));
        activeToggle.setVisible(true);
      }

      @Override
      public void onFailure(Throwable caught) {
        // never invoked
      }
    }));

    activeToggle.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
          new RestApi("config").id("server")
              .view(Plugin.get().getPluginName(), "serviceusers")
              .id(serviceUser).view("active")
              .put(new AsyncCallback<NoContent>() {
                @Override
                public void onSuccess(NoContent result) {
                }

                @Override
                public void onFailure(Throwable caught) {
                  // never invoked
                }
              });
        } else {
          new RestApi("config").id("server")
              .view(Plugin.get().getPluginName(), "serviceusers")
              .id(serviceUser).view("active")
              .delete(new AsyncCallback<NoContent>() {
                @Override
                public void onSuccess(NoContent result) {
                }

                @Override
                public void onFailure(Throwable caught) {
                  // never invoked
                }
              });
        }
      }
    });

    return activeToggle;
  }

  private static class MyTable extends FlexTable {
    private static int row = 0;

    private void addRow(String label, String value) {
      setWidget(row, 0, new Label(label + ":"));
      setWidget(row, 1, new Label(value));
      row++;
    }

    private void addRow(String label, Widget w) {
      setWidget(row, 0, new Label(label + ":"));
      setWidget(row, 1, w);
      row++;
    }
  }
}

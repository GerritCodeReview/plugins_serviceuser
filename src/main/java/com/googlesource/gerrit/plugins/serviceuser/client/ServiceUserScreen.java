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

import com.google.gerrit.client.rpc.NativeString;
import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.rpc.NoContent;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtexpui.clippy.client.CopyableLabel;

public class ServiceUserScreen extends VerticalPanel {
  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Service User " + screen.getToken(1));
      screen.show(new ServiceUserScreen(screen.getToken(1)));
    }
  }

  ServiceUserScreen(final String serviceUser) {
    setStyleName("serviceuser-panel");

    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "serviceusers")
        .id(serviceUser).get(new AsyncCallback<ServiceUserInfo>() {
            @Override
            public void onSuccess(final ServiceUserInfo serviceUserInfo) {
              new RestApi("config").id("server").view(Plugin.get().getPluginName(), "serviceusers")
                  .id(serviceUser).view("password.http").get(new AsyncCallback<MyNativeString>() {
                      @Override
                      public void onSuccess(final MyNativeString httpPassword) {
                        new RestApi("config").id("server")
                            .view(Plugin.get().getPluginName(), "config")
                            .get(new AsyncCallback<ConfigInfo>() {
                              @Override
                              public void onSuccess(final ConfigInfo configInfo) {
                                AccountCapabilities.all(new AsyncCallback<AccountCapabilities>() {
                                  @Override
                                  public void onSuccess(AccountCapabilities ac) {
                                    boolean isAdmin = ac.canPerform("administrateServer");
                                    display(serviceUserInfo,
                                        httpPassword.asString(),
                                        configInfo.getAllowEmail() || isAdmin,
                                        configInfo.getAllowOwner() || isAdmin,
                                        configInfo.getAllowHttpPassword() || isAdmin);
                                  }

                                  @Override
                                  public void onFailure(Throwable caught) {
                                    // never invoked
                                  }
                                }, "administrateServer");
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

            @Override
            public void onFailure(Throwable caught) {
              // never invoked
            }
          });
  }

  private void display(ServiceUserInfo info, String httpPassword,
      boolean allowEmail, boolean allowOwner, boolean allowHttpPassword) {
    MyTable t = new MyTable();
    t.setStyleName("serviceuser-serviceUserInfoTable");
    t.addRow("Account State", createActiveToggle(info));
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
    t.addRow(
        "HTTP Password",
        createHttpPasswordWidget(info.username(), httpPassword,
            allowHttpPassword));
    t.addRow("Owner Group", createOwnerWidget(info, allowOwner));
    t.addRow("Created By", info.getDisplayName());
    t.addRow("Created At", info.created_at());
    add(t);

    add(new SshPanel(info.username()));
  }

  private ToggleButton createActiveToggle(final ServiceUserInfo info) {
    final ToggleButton activeToggle = new ToggleButton();
    activeToggle.setStyleName("serviceuser-toggleButton");
    activeToggle.setVisible(false);
    activeToggle.setValue(true);
    activeToggle.setText("Active");
    activeToggle.setValue(false);
    activeToggle.setText("Inactive");
    activeToggle.setValue(info.active());
    activeToggle.setVisible(true);

    activeToggle.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
          new RestApi("config").id("server")
              .view(Plugin.get().getPluginName(), "serviceusers")
              .id(info.username()).view("active")
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
              .id(info.username()).view("active")
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

  private Widget createHttpPasswordWidget(final String serviceUser,
      String httpPassword, boolean allowHttpPassword) {
    if (allowHttpPassword) {
      HorizontalPanel p = new HorizontalPanel();
      final CopyableLabel label = new CopyableLabel(httpPassword);
      label.setVisible(!httpPassword.isEmpty());
      p.add(label);

      final Image delete = new Image(ServiceUserPlugin.RESOURCES.deleteHover());
      delete.addStyleName("serviceuser-deleteButton");
      delete.setTitle("Clear HTTP password");
      delete.addClickHandler(new  ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          new RestApi("config").id("server").view(Plugin.get().getPluginName(), "serviceusers")
              .id(serviceUser).view("password.http").delete(new AsyncCallback<NoContent>() {
                  @Override
                  public void onSuccess(NoContent noContent) {
                    label.setText("");
                    label.setVisible(false);
                    delete.setVisible(false);
                  }

                  @Override
                  public void onFailure(Throwable caught) {
                    // never invoked
                  }
              });
        }
      });
      delete.setVisible(!httpPassword.isEmpty());
      p.add(delete);

      Image generate = new Image(ServiceUserPlugin.RESOURCES.gear());
      generate.addStyleName("serviceuser-generateButton");
      generate.setTitle("Generate new HTTP password");
      generate.addClickHandler(new  ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          HttpPasswordInput in = HttpPasswordInput.create();
          in.generate(true);
          new RestApi("config").id("server").view(Plugin.get().getPluginName(), "serviceusers")
              .id(serviceUser).view("password.http").put(in, new AsyncCallback<MyNativeString>() {
                  @Override
                  public void onSuccess(MyNativeString newPassword) {
                    label.setText(newPassword.asString());
                    label.setVisible(true);
                    delete.setVisible(true);
                  }

                  @Override
                  public void onFailure(Throwable caught) {
                    // never invoked
                  }
              });
        }
      });
      p.add(generate);
      return p;
    } else {
      return new CopyableLabel(httpPassword);
    }
  }

  private Widget createOwnerWidget(ServiceUserInfo info, boolean allowOwner) {
    if (allowOwner) {
      EditableValue ownerWidget = new EditableValue(info.username(),
          info.owner() != null ? info.owner().name() : "",
          info.owner() != null ? info.owner().url() : null) {
        @Override
        protected void save(String serviceUser, final String newValue) {
          new RestApi("config").id("server")
              .view(Plugin.get().getPluginName(), "serviceusers").id(serviceUser)
              .view("owner").put(newValue, new AsyncCallback<GroupInfo>() {
                @Override
                public void onSuccess(GroupInfo result) {
                  updateValue(result != null ? result.name() : "");
                  updateHref(result != null ? result.url() : "");
                  Plugin.get().refresh();
                }

                @Override
                public void onFailure(Throwable caught) {
                  // never invoked
                }
              });
        }
      };
      StringBuilder ownerWarning = new StringBuilder();
      ownerWarning.append("If ");
      ownerWarning.append(info.owner() != null
          ? "the owner group is changed"
          : "an owner group is set");
      ownerWarning.append(" only members of the ");
      ownerWarning.append(info.owner() != null ? "new " : "");
      ownerWarning.append("owner group can see and administrate"
          + " the service user.");
      if (info.owner() != null) {
        ownerWarning.append(" If the owner group is removed only the"
            + " creator of the service user can see and administrate"
            + " the service user.");
      } else {
        ownerWarning.append(" The creator of the service user can no"
            + " longer see and administrate the service user if she/he"
            + " is not member of the owner group.");
      }
      ownerWidget.setWarning(ownerWarning.toString());
      return ownerWidget;
    } else {
      if (info.owner() != null && info.owner().url() != null) {
        return new Anchor(info.owner().name(), info.owner().url());
      } else {
        return new Label(info.owner() != null ? info.owner().name() : "");
      }
    }
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

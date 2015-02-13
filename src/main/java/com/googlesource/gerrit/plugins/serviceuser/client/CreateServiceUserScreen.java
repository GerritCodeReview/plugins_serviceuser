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

package com.googlesource.gerrit.plugins.serviceuser.client;

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CreateServiceUserScreen extends VerticalPanel {
  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Create Service User");
      screen.show(new CreateServiceUserScreen());
    }
  }

  private TextBox usernameTxt;
  private TextBox emailTxt;
  private TextArea sshKeyTxt;
  private String onSuccessMessage;

  CreateServiceUserScreen() {
    setStyleName("serviceuser-panel");

    Panel usernamePanel = new VerticalPanel();
    usernamePanel.add(new Label("Username:"));
    usernameTxt = new TextBox() {
      @Override
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        if (event.getTypeInt() == Event.ONPASTE) {
          Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
              if (getValue().trim().length() != 0) {
                setEnabled(true);
              }
            }
          });
        }
      }
    };
    usernameTxt.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(final KeyPressEvent event) {
        event.stopPropagation();
      }
    });
    usernameTxt.sinkEvents(Event.ONPASTE);
    usernameTxt.setVisibleLength(40);
    usernamePanel.add(usernameTxt);
    add(usernamePanel);

    Panel sshKeyPanel = new VerticalPanel();
    sshKeyPanel.add(new Label("Public SSH Key:"));
    sshKeyPanel.add(new SshKeyHelpPanel());
    sshKeyTxt = new TextArea();
    sshKeyTxt.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(final KeyPressEvent event) {
        event.stopPropagation();
      }
    });
    sshKeyTxt.setVisibleLines(12);
    sshKeyTxt.setCharacterWidth(80);
    sshKeyTxt.getElement().setPropertyBoolean("spellcheck", false);
    sshKeyPanel.add(sshKeyTxt);
    add(sshKeyPanel);

    HorizontalPanel buttons = new HorizontalPanel();
    add(buttons);

    final Button createButton = new Button("Create");
    createButton.addStyleName("serviceuser-createButton");
    createButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        doCreate();
      }
    });
    buttons.add(createButton);
    createButton.setEnabled(false);
    new OnEditEnabler(createButton, usernameTxt);

    usernameTxt.setFocus(true);
    createButton.setEnabled(false);

    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "config")
        .get(new AsyncCallback<ConfigInfo>() {
          @Override
          public void onSuccess(ConfigInfo info) {
            onSuccessMessage = info.getOnSuccessMessage();

            String infoMessage = info.getInfoMessage();
            if (infoMessage != null && !"".equals(infoMessage)) {
              insert(new HTML(infoMessage), 0);
            }

            if (info.getAllowEmail()) {
              Panel emailPanel = new VerticalPanel();
              emailPanel.add(new Label("Email:"));
              emailTxt = new TextBox() {
                @Override
                public void onBrowserEvent(Event event) {
                  super.onBrowserEvent(event);
                  if (event.getTypeInt() == Event.ONPASTE) {
                    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                      @Override
                      public void execute() {
                        if (getValue().trim().length() != 0) {
                          setEnabled(true);
                        }
                      }
                    });
                  }
                }
              };
              emailTxt.addKeyPressHandler(new KeyPressHandler() {
                @Override
                public void onKeyPress(final KeyPressEvent event) {
                  event.stopPropagation();
                }
              });
              emailTxt.sinkEvents(Event.ONPASTE);
              emailTxt.setVisibleLength(40);
              emailPanel.add(emailTxt);
              insert(emailPanel, 2);
            }
          }

          @Override
          public void onFailure(Throwable caught) {
            // never invoked
          }
    });
  }

  private void doCreate() {
    final String username = usernameTxt.getValue().trim();
    String sshKey = sshKeyTxt.getText();
    if (sshKey != null) {
      sshKey = sshKey.trim();
    }

    ServiceUserInput in = ServiceUserInput.create();
    in.ssh_key(sshKey);
    if (emailTxt != null) {
      in.email(emailTxt.getValue().trim());
    }
    new RestApi("config").id("server").view("serviceuser", "serviceusers")
        .id(username).post(in, new AsyncCallback<JavaScriptObject>() {

      @Override
      public void onSuccess(JavaScriptObject result) {
        clearForm();
        Plugin.get().go("/x/" + Plugin.get().getName() + "/user/" + username);

        final DialogBox successDialog = new DialogBox();
        successDialog.setText("Service User Created");
        successDialog.setAnimationEnabled(true);

        Panel p = new VerticalPanel();
        p.setStyleName("serviceuser-panel");
        p.add(new Label("The service user '" + username + "' was created."));
        Button okButton = new Button("OK");
        okButton.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            successDialog.hide();
          }
        });

        if (onSuccessMessage != null && !"".equals(onSuccessMessage)) {
          p.add(new HTML(onSuccessMessage));
        }

        p.add(okButton);
        successDialog.add(p);

        successDialog.center();
        successDialog.show();
      }

      @Override
      public void onFailure(Throwable caught) {
      }
    });
  }

  private void clearForm() {
    usernameTxt.setValue("");
    if (emailTxt != null) {
      emailTxt.setValue("");
    }
    sshKeyTxt.setValue("");
  }

  private static class ServiceUserInput extends JavaScriptObject {
    final native void ssh_key(String s) /*-{ this.ssh_key = s; }-*/;
    final native void email(String e) /*-{ this.email = e; }-*/;

    static ServiceUserInput create() {
      ServiceUserInput g = (ServiceUserInput) createObject();
      return g;
    }

    protected ServiceUserInput() {
    }
  }
}

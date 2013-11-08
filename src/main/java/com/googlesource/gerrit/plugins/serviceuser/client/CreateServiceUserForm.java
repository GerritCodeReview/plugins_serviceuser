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
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.googlesource.gerrit.plugins.serviceuser.CreateServiceUserMenu;

public class CreateServiceUserForm extends Plugin {
  private DialogBox dialogBox;
  private TextBox usernameTxt;
  private TextArea sshKeyTxt;

  @Override
  public void onModuleLoad() {
    dialogBox = new DialogBox(false, false);
    dialogBox.setText("Create Service User");
    dialogBox.setAnimationEnabled(true);

    Panel p = new VerticalPanel();
    p.setStyleName("panel");

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
    p.add(usernamePanel);

    Panel sshKeyPanel = new VerticalPanel();
    sshKeyPanel.add(new Label("Public SSH Key:"));
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
    p.add(sshKeyPanel);

    HorizontalPanel buttons = new HorizontalPanel();
    p.add(buttons);

    Button createButton = new Button("Create");
    createButton.addStyleName("createButton");
    createButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        doCreate();
      }
    });
    buttons.add(createButton);
    createButton.setEnabled(false);
    new OnEditEnabler(createButton, usernameTxt);

    Button closeButton = new Button("Close");
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        hide();
      }
    });
    buttons.add(closeButton);

    dialogBox.setWidget(p);

    RootPanel rootPanel = RootPanel.get(CreateServiceUserMenu.MENU_ID);
    rootPanel.getElement().removeAttribute("href");
    rootPanel.addDomHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          dialogBox.center();
          dialogBox.show();
          usernameTxt.setFocus(true);
        }
    }, ClickEvent.getType());
  }

  private void doCreate() {
    final String username = usernameTxt.getValue().trim();
    String sshKey = sshKeyTxt.getText();
    if (sshKey != null) {
      sshKey = sshKey.trim();
    }

    ServiceUserInput in = ServiceUserInput.create();
    in.ssh_key(sshKey);
    new RestApi("config").id("server").view("serviceuser", "serviceusers").id(username).post(in,
        new AsyncCallback<JavaScriptObject>() {

      @Override
      public void onSuccess(JavaScriptObject result) {
        hide();

        final DialogBox successDialog = new DialogBox();
        successDialog.setText("Service User Created");
        successDialog.setAnimationEnabled(true);

        Panel p = new VerticalPanel();
        p.setStyleName("panel");
        p.add(new Label("The service user '" + username + "' was created."));
        Button okButton = new Button("OK");
        okButton.addStyleName("okButton");
        okButton.addClickHandler(new ClickHandler() {
          public void onClick(ClickEvent event) {
            successDialog.hide();
          }
        });
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

  private void hide() {
    dialogBox.hide();
    usernameTxt.setValue("");
    sshKeyTxt.setValue("");
  }

  private static class ServiceUserInput extends JavaScriptObject {
    final native void ssh_key(String s) /*-{ this.ssh_key = s; }-*/;

    static ServiceUserInput create() {
      ServiceUserInput g = (ServiceUserInput) createObject();
      return g;
    }

    protected ServiceUserInput() {
    }
  }
}

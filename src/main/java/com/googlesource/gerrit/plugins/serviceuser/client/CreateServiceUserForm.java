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
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CreateServiceUserForm extends Plugin {
  private TextBox usernameTxt;
  private TextArea sshKeyTxt;
  private String onSuccessMessage;

  @Override
  public void onPluginLoad() {
    Plugin.screen("create-service-user", new Screen.Callback() {
      @Override
      public void onLoad(Screen screen) {
        final VerticalPanel p = new VerticalPanel();
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
        DisclosurePanel dp = new DisclosurePanel("How to generate an SSH Key");
        StringBuilder b = new StringBuilder();
        b.append("<ol>")
            .append("<li>From the Terminal or Git Bash, run <em>ssh-keygen</em></li>")
            .append("<li>")
                .append("Enter a path for the key, e.g. <em>id_rsa</em>. If you are generating the key<br />")
                .append("on your local system take care to not overwrite your own SSH key.")
            .append("</li>")
            .append("<li>")
                .append("Enter a passphrase only if the service where you intend to use this<br />")
                .append("service user is able to deal with passphrases, otherwise leave it blank.<br />")
                .append("Remember this passphrase, as you will need it to unlock the key.")
            .append("</li>")
            .append("<li>")
                .append("Open <em>id_rsa.pub</em> and copy &amp; paste the contents into the box below.<br />")
                .append("Note that <em>id_rsa.pub</em> is your public key and can be shared,<br />")
                .append("while <em>id_rsa</em> is your private key and should be kept secret.")
            .append("</li>")
         .append("</ol>");
        dp.add(new HTML(b.toString()));
        sshKeyPanel.add(dp);
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

        final Button createButton = new Button("Create");
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

        usernameTxt.setFocus(true);
        createButton.setEnabled(false);

        new RestApi("config").id("server").view("serviceuser", "messages")
            .get(new AsyncCallback<MessagesInfo>() {
              @Override
              public void onSuccess(MessagesInfo info) {
                onSuccessMessage = info.getOnSuccessMessage();

                String infoMessage = info.getInfoMessage();
                if (infoMessage != null && !"".equals(infoMessage)) {
                  p.insert(new HTML(infoMessage), 0);
                }
              }

              @Override
              public void onFailure(Throwable caught) {
                // never invoked
              }
        });

        screen.show();
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
    new RestApi("config").id("server").view("serviceuser", "serviceusers")
        .id(username).post(in, new AsyncCallback<JavaScriptObject>() {

      @Override
      public void onSuccess(JavaScriptObject result) {
        clear();

        final DialogBox successDialog = new DialogBox();
        successDialog.setText("Service User Created");
        successDialog.setAnimationEnabled(true);

        Panel p = new VerticalPanel();
        p.setStyleName("panel");
        p.add(new Label("The service user '" + username + "' was created."));
        Button okButton = new Button("OK");
        okButton.addClickHandler(new ClickHandler() {
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

  private void clear() {
    usernameTxt.setValue("");
    sshKeyTxt.setValue("");
  }

  private static class MessagesInfo extends JavaScriptObject {
    public final native String getOnSuccessMessage() /*-{ return this.on_success }-*/;
    public final native String getInfoMessage() /*-{ return this.info }-*/;

    protected MessagesInfo() {
    }
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

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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ServiceUserAdminScreen extends VerticalPanel {
  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Service User Administration");
      screen.show(new ServiceUserAdminScreen());
    }
  }

  private TextArea infoMsgTxt;
  private TextArea onSuccessMsgTxt;
  private Button saveButton;

  ServiceUserAdminScreen() {
    setStyleName("serviceuser-panel");

    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "messages")
        .get(new AsyncCallback<MessagesInfo>() {
          @Override
          public void onSuccess(MessagesInfo info) {
            display(info);
          }

          @Override
          public void onFailure(Throwable caught) {
            // never invoked
          }
        });
  }

  private void display(MessagesInfo info) {
    Panel infoMsgPanel = new VerticalPanel();
    Panel infoMsgTitelPanel = new HorizontalPanel();
    infoMsgTitelPanel.add(new Label("Info Message"));
    Image infoMsgInfo = new Image(ServiceUserPlugin.RESOURCES.info());
    infoMsgInfo.setTitle("HTML formatted message that should be"
        + " displayed in the service user creation dialog.");
    infoMsgTitelPanel.add(infoMsgInfo);
    infoMsgTitelPanel.add(new Label(":"));
    infoMsgPanel.add(infoMsgTitelPanel);
    infoMsgTxt = new TextArea();
    infoMsgTxt.setValue(info.getInfoMessage());
    infoMsgTxt.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(final KeyPressEvent event) {
        event.stopPropagation();
      }
    });
    infoMsgTxt.setVisibleLines(12);
    infoMsgTxt.setCharacterWidth(80);
    infoMsgTxt.getElement().setPropertyBoolean("spellcheck", false);
    infoMsgPanel.add(infoMsgTxt);
    add(infoMsgPanel);

    Panel onSuccessMsgPanel = new VerticalPanel();
    Panel onSuccessMsgTitelPanel = new HorizontalPanel();
    onSuccessMsgTitelPanel.add(new Label("On Success Message"));
    Image onSuccessMsgInfo = new Image(ServiceUserPlugin.RESOURCES.info());
    onSuccessMsgInfo.setTitle("HTML formatted message that should be"
        + " displayed after a service user was successfully created.");
    onSuccessMsgTitelPanel.add(onSuccessMsgInfo);
    onSuccessMsgTitelPanel.add(new Label(":"));
    onSuccessMsgPanel.add(onSuccessMsgTitelPanel);
    onSuccessMsgTxt = new TextArea();
    onSuccessMsgTxt.setValue(info.getOnSuccessMessage());
    onSuccessMsgTxt.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(final KeyPressEvent event) {
        event.stopPropagation();
      }
    });
    onSuccessMsgTxt.setVisibleLines(12);
    onSuccessMsgTxt.setCharacterWidth(80);
    onSuccessMsgTxt.getElement().setPropertyBoolean("spellcheck", false);
    onSuccessMsgPanel.add(onSuccessMsgTxt);
    add(onSuccessMsgPanel);

    HorizontalPanel buttons = new HorizontalPanel();
    add(buttons);

    saveButton = new Button("Save");
    saveButton.addStyleName("serviceuser-saveButton");
    saveButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        doSave();
      }
    });
    buttons.add(saveButton);
    saveButton.setEnabled(false);
    OnEditEnabler onEditEnabler = new OnEditEnabler(saveButton, infoMsgTxt);
    onEditEnabler.listenTo(onSuccessMsgTxt);

    infoMsgTxt.setFocus(true);
    saveButton.setEnabled(false);
  }

  private void doSave() {
    MessagesInfo in = MessagesInfo.create();
    in.setInfoMessage(infoMsgTxt.getValue());
    in.setOnSuccessMessage(onSuccessMsgTxt.getValue());
    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "messages")
        .put(in, new AsyncCallback<JavaScriptObject>() {

          @Override
          public void onSuccess(JavaScriptObject result) {
            saveButton.setEnabled(false);
          }

          @Override
          public void onFailure(Throwable caught) {
            // never invoked
          }
        });
  }
}

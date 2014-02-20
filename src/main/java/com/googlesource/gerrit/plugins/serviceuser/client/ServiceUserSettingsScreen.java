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
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ServiceUserSettingsScreen extends VerticalPanel {
  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Service User Administration");
      screen.show(new ServiceUserSettingsScreen());
    }
  }

  private TextArea infoMsgTxt;
  private TextArea onSuccessMsgTxt;
  private CheckBox allowEmailCheckBox;
  private CheckBox createNotesCheckBox;
  private CheckBox createNotesAsyncCheckBox;
  private Button saveButton;

  ServiceUserSettingsScreen() {
    setStyleName("serviceuser-panel");

    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "config")
        .get(new AsyncCallback<ConfigInfo>() {
          @Override
          public void onSuccess(ConfigInfo info) {
            display(info);
          }

          @Override
          public void onFailure(Throwable caught) {
            // never invoked
          }
        });
  }

  private void display(ConfigInfo info) {
    Panel infoMsgPanel = new VerticalPanel();
    Panel infoMsgTitelPanel = new HorizontalPanel();
    infoMsgTitelPanel.add(new Label("Info Message"));
    Image infoMsgInfo = new Image(ServiceUserPlugin.RESOURCES.info());
    infoMsgInfo.setTitle("HTML formatted message that should be"
        + " displayed on the service user creation screen.");
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

    Panel allowEmailPanel = new HorizontalPanel();
    allowEmailCheckBox = new CheckBox("Allow Email Address");
    allowEmailCheckBox.setValue(info.getAllowEmail());
    allowEmailPanel.add(allowEmailCheckBox);
    Image allowEmailInfo = new Image(ServiceUserPlugin.RESOURCES.info());
    allowEmailInfo.setTitle("Whether it is allowed to provide an email address "
        + "for a service user. E.g. having an email address allows a service user "
        + "to push commits and tags.");
    allowEmailPanel.add(allowEmailInfo);
    add(allowEmailPanel);

    Panel createNotesPanel = new HorizontalPanel();
    createNotesCheckBox = new CheckBox("Create Git Notes");
    createNotesCheckBox.setValue(info.getCreateNotes());
    createNotesPanel.add(createNotesCheckBox);
    Image createNotesInfo = new Image(ServiceUserPlugin.RESOURCES.info());
    createNotesInfo.setTitle("Whether commits of a service user should be "
        + "annotated by a Git note that contains information about the current "
        + "owners of the service user. This allows to find a real person that "
        + "is responsible for this commit. To get such a Git note for each commit "
        + "of a service user the 'Forge Committer' access right must be blocked "
        + "for service users.");
    createNotesPanel.add(createNotesInfo);
    add(createNotesPanel);

    Panel createNotesAsyncPanel = new HorizontalPanel();
    createNotesAsyncCheckBox = new CheckBox("Create Git Notes Asynchronously");
    createNotesAsyncCheckBox.setValue(info.getCreateNotesAsync());
    createNotesAsyncCheckBox.setEnabled(info.getCreateNotes());
    createNotesAsyncPanel.add(createNotesAsyncCheckBox);
    Image createNotesAsyncInfo = new Image(ServiceUserPlugin.RESOURCES.info());
    createNotesAsyncInfo.setTitle("Whether the Git notes on commits that are "
        + "pushed by a service user should be created asynchronously.");
    createNotesAsyncPanel.add(createNotesAsyncInfo);
    add(createNotesAsyncPanel);

    createNotesCheckBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        createNotesAsyncCheckBox.setEnabled(event.getValue());
      }
    });

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
    onEditEnabler.listenTo(allowEmailCheckBox);
    onEditEnabler.listenTo(createNotesCheckBox);
    onEditEnabler.listenTo(createNotesAsyncCheckBox);

    infoMsgTxt.setFocus(true);
    saveButton.setEnabled(false);
  }

  private void doSave() {
    ConfigInfo in = ConfigInfo.create();
    in.setInfoMessage(infoMsgTxt.getValue());
    in.setOnSuccessMessage(onSuccessMsgTxt.getValue());
    in.setAllowEmail(allowEmailCheckBox.getValue());
    in.setCreateNotes(createNotesCheckBox.getValue());
    if (createNotesAsyncCheckBox.isEnabled()) {
      in.setCreateNotesAsync(createNotesAsyncCheckBox.getValue());
    }
    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "config")
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

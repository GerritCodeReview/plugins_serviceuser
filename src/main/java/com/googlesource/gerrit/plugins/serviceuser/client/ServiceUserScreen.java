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
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtexpui.globalkey.client.NpTextBox;

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
    t.addRow("Full Name", getNameWidget(info.username(), info.name()));
    t.addRow("Email Address", info.email());
    t.addRow("Created By", info.created_by());
    t.addRow("Created At", info.created_at());
    add(t);

    add(new SshPanel(info.username()));
  }

  private Widget getNameWidget(final String serviceUser, final String name) {
    FlowPanel p = new FlowPanel();
    final InlineLabel l = new InlineLabel(name);
    final Image edit = new Image(ServiceUserPlugin.RESOURCES.edit());
    edit.addStyleName("serviceuser-editNameButton");

    final NpTextBox input = new NpTextBox();
    input.setVisibleLength(25);
    input.setValue(name);
    input.setVisible(false);
    final Button save = new Button();
    save.setText("Save");
    save.setVisible(false);
    save.setEnabled(false);
    final Button cancel = new Button();
    cancel.setText("Cancel");
    cancel.setVisible(false);

    OnEditEnabler e = new OnEditEnabler(save);
    e.listenTo(input);

    edit.addClickHandler(new  ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        l.setVisible(false);
        edit.setVisible(false);
        input.setVisible(true);
        save.setVisible(true);
        cancel.setVisible(true);
      }
    });
    save.addClickHandler(new  ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        save.setEnabled(false);
        final String newName = input.getValue().trim();
        new RestApi("config").id("server").view(Plugin.get().getPluginName(), "serviceusers").id(serviceUser)
            .view("name").put(newName, new AsyncCallback<NativeString>() {
          @Override
          public void onSuccess(NativeString result) {
            l.setText(newName);
            l.setVisible(true);
            edit.setVisible(true);
            input.setVisible(false);
            input.setValue(newName);
            save.setVisible(false);
            save.setEnabled(false);
            cancel.setVisible(false);
          }

          @Override
          public void onFailure(Throwable caught) {
            // never invoked
          }
        });
      }
    });
    cancel.addClickHandler(new  ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        l.setVisible(true);
        edit.setVisible(true);
        input.setVisible(false);
        input.setValue(l.getText());
        save.setVisible(false);
        save.setEnabled(false);
        cancel.setVisible(false);
      }
    });

    p.add(l);
    p.add(edit);
    p.add(input);
    p.add(save);
    p.add(cancel);
    return p;
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

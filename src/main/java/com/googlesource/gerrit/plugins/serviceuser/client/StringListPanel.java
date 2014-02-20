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

import com.google.gerrit.client.rpc.Natives;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwtexpui.globalkey.client.NpTextBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StringListPanel extends FlowPanel {
  private final NpTextBox input;
  private final StringListTable t;
  private final Button deleteButton;
  private final HorizontalPanel titlePanel;
  private Image info;

  StringListPanel(String title, String fieldName, JsArrayString values,
      final FocusWidget w) {
    this(title, fieldName, Natives.asList(values), w);
  }

  StringListPanel(String title, String fieldName, Collection<String> values,
      final FocusWidget w) {
    titlePanel = new HorizontalPanel();
    Label titleLabel = new Label(title);
    titleLabel.setStyleName("serviceuser-smallHeading");
    titlePanel.add(titleLabel);
    add(titlePanel);
    input = new NpTextBox();
    input.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
          w.setEnabled(true);
          add();
        }
      }
    });
    HorizontalPanel p = new HorizontalPanel();
    p.add(input);
    Button addButton = new Button("Add");
    addButton.setEnabled(false);
    new OnEditEnabler(addButton, input);
    addButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        w.setEnabled(true);
        add();
      }
    });
    p.add(addButton);
    add(p);

    t = new StringListTable(fieldName);
    add(t);

    deleteButton = new Button("Delete");
    deleteButton.setEnabled(false);
    add(deleteButton);
    deleteButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        w.setEnabled(true);
        t.deleteChecked();
      }
    });

    t.display(values);
  }

  void setInfo(String msg) {
    if (info == null) {
      info = new Image(ServiceUserPlugin.RESOURCES.info());
      titlePanel.add(info);
    }
    info.setTitle(msg);
  }

  List<String> getValues() {
    return t.getValues();
  }

  private void add() {
    String v = input.getValue().trim();
    if (!v.isEmpty()) {
      input.setValue("");
      t.insert(v);
    }
  }

  private class StringListTable extends FlexTable {
    StringListTable(String name) {
      setStyleName("serviceuser-stringListTable");
      FlexCellFormatter fmt = getFlexCellFormatter();
      fmt.addStyleName(0, 0, "iconHeader");
      fmt.addStyleName(0, 0, "topMostCell");
      fmt.addStyleName(0, 0, "leftMostCell");
      fmt.addStyleName(0, 1, "dataHeader");
      fmt.addStyleName(0, 1, "topMostCell");

      setText(0, 1, name);
    }

    void display(Collection<String> values) {
      int row = 1;
      for (String v : values) {
        populate(row, v);
        row++;
      }
    }

    List<String> getValues() {
      List<String> values = new ArrayList<String>();
      for (int row = 1; row < getRowCount(); row++) {
        values.add(getText(row, 1));
      }
      return values;
    }

    private void populate(int row, String value) {
      FlexCellFormatter fmt = getFlexCellFormatter();
      fmt.addStyleName(row, 0, "leftMostCell");
      fmt.addStyleName(row, 0, "iconCell");
      fmt.addStyleName(row, 1, "dataCell");

      CheckBox checkBox = new CheckBox();
      checkBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
        @Override
        public void onValueChange(ValueChangeEvent<Boolean> event) {
          enableDelete();
        }
      });
      setWidget(row, 0, checkBox);
      setText(row, 1, value);
    }

    void insert(String v) {
      int insertPos = getRowCount();
      for (int row = 1; row < getRowCount(); row++) {
        int compareResult = v.compareTo(getText(row, 1));
        if (compareResult < 0)  {
          insertPos = row;
          break;
        } else if (compareResult == 0) {
          return;
        }
      }
      insertRow(insertPos);
      populate(insertPos, v);
    }

    void enableDelete() {
      for (int row = 1; row < getRowCount(); row++) {
        if (((CheckBox) getWidget(row, 0)).getValue()) {
          deleteButton.setEnabled(true);
          return;
        }
      }
      deleteButton.setEnabled(false);
    }

    void deleteChecked() {
      deleteButton.setEnabled(false);
      for (int row = 1; row < getRowCount(); row++) {
        if (((CheckBox) getWidget(row, 0)).getValue()) {
          removeRow(row--);
        }
      }
    }
  }
}

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
import com.google.gerrit.plugin.client.rpc.Natives;
import com.google.gerrit.plugin.client.rpc.NoContent;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwtexpui.clippy.client.CopyableLabel;
import com.google.gwtexpui.globalkey.client.NpTextArea;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

class SshPanel extends Composite {
  private final String serviceUser;

  private SshKeyTable keys;

  private Button showAddKeyBlock;
  private Panel addKeyBlock;
  private Button closeAddKeyBlock;
  private Button clearNew;
  private Button addNew;
  private NpTextArea addTxt;
  private Button deleteKey;

  private Panel serverKeys;

  private int loadCount;

  SshPanel(String serviceUser) {
    this.serviceUser = serviceUser;

    FlowPanel body = new FlowPanel();

    showAddKeyBlock = new Button("Add Key ...");
    showAddKeyBlock.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        showAddKeyBlock(true);
      }
    });

    keys = new SshKeyTable();
    body.add(keys);
    {
      final FlowPanel fp = new FlowPanel();
      deleteKey = new Button("Delete");
      deleteKey.setEnabled(false);
      deleteKey.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(final ClickEvent event) {
          keys.deleteChecked();
        }
      });
      fp.add(deleteKey);
      fp.add(showAddKeyBlock);
      body.add(fp);
    }

    addKeyBlock = new VerticalPanel();
    addKeyBlock.setVisible(false);
    addKeyBlock.setStyleName("serviceuser-addSshKeyPanel");
    addKeyBlock.add(new Label("Add SSH Public Key"));
    addKeyBlock.add(new SshKeyHelpPanel());

    addTxt = new NpTextArea();
    addTxt.setVisibleLines(12);
    addTxt.setCharacterWidth(80);
    addTxt.setSpellCheck(false);
    addKeyBlock.add(addTxt);

    final HorizontalPanel buttons = new HorizontalPanel();
    addKeyBlock.add(buttons);

    clearNew = new Button("Clear");
    clearNew.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        addTxt.setText("");
        addTxt.setFocus(true);
      }
    });
    buttons.add(clearNew);

    addNew = new Button("Add");
    addNew.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        doAddNew();
      }
    });
    buttons.add(addNew);

    closeAddKeyBlock = new Button("Close");
    closeAddKeyBlock.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        showAddKeyBlock(false);
      }
    });
    buttons.add(closeAddKeyBlock);
    buttons.setCellWidth(closeAddKeyBlock, "100%");
    buttons.setCellHorizontalAlignment(closeAddKeyBlock,
        HasHorizontalAlignment.ALIGN_RIGHT);

    body.add(addKeyBlock);

    serverKeys = new FlowPanel();
    body.add(serverKeys);

    initWidget(body);
  }

  void setKeyTableVisible(final boolean on) {
    keys.setVisible(on);
    deleteKey.setVisible(on);
    closeAddKeyBlock.setVisible(on);
  }

  void doAddNew() {
    final String txt = addTxt.getText();
    if (txt != null && txt.length() > 0) {
      new RestApi("config").id("server")
          .view(Plugin.get().getPluginName(), "serviceusers").id(serviceUser)
          .view("sshkeys").post(txt, new AsyncCallback<SshKeyInfo>() {
        public void onSuccess(SshKeyInfo k) {
          addTxt.setText("");
          keys.addOneKey(k);
          if (!keys.isVisible()) {
            showAddKeyBlock(false);
            setKeyTableVisible(true);
            keys.updateDeleteButton();
          }
        }

        @Override
        public void onFailure(final Throwable caught) {
          // never invoked
        }
      });
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    refreshSshKeys();
  }

  private void refreshSshKeys() {
    new RestApi("config").id("server")
        .view(Plugin.get().getPluginName(), "serviceusers").id(serviceUser)
        .view("sshkeys").get(new AsyncCallback<JsArray<SshKeyInfo>>() {
      @Override
      public void onSuccess(JsArray<SshKeyInfo> result) {
        keys.display(Natives.asList(result));
        if (result.length() == 0 && keys.isVisible()) {
          showAddKeyBlock(true);
        }
        if (++loadCount == 2) {
          display();
        }
      }

      @Override
      public void onFailure(Throwable caught) {
        // never invoked
      }
    });
  }

  void display() {
  }

  private void showAddKeyBlock(boolean show) {
    showAddKeyBlock.setVisible(!show);
    addKeyBlock.setVisible(show);
  }

  private class SshKeyTable extends FlexTable {
    private final Map<Integer, SshKeyInfo> sshKeyInfos;
    private ValueChangeHandler<Boolean> updateDeleteHandler;

    SshKeyTable() {
      this.sshKeyInfos = new HashMap<Integer, SshKeyInfo>();
      setStyleName("serviceuser-sshKeyTable");
      setWidth("");
      setText(0, 2, "Status");
      setText(0, 3, "Algorithm");
      setText(0, 4, "Key");
      setText(0, 5, "Comment");

      FlexCellFormatter fmt = getFlexCellFormatter();
      fmt.addStyleName(0, 1, "iconHeader");
      fmt.addStyleName(0, 2, "dataHeader");
      fmt.addStyleName(0, 3, "dataHeader");
      fmt.addStyleName(0, 4, "dataHeader");
      fmt.addStyleName(0, 5, "dataHeader");

      fmt.addStyleName(0, 1, "topMostCell");
      fmt.addStyleName(0, 2, "topMostCell");
      fmt.addStyleName(0, 3, "topMostCell");
      fmt.addStyleName(0, 4, "topMostCell");
      fmt.addStyleName(0, 5, "topMostCell");

      updateDeleteHandler = new ValueChangeHandler<Boolean>() {
        @Override
        public void onValueChange(ValueChangeEvent<Boolean> event) {
          updateDeleteButton();
        }
      };
    }

    void deleteChecked() {
      final HashSet<Integer> sequenceNumbers = new HashSet<Integer>();
      for (int row = 1; row < getRowCount(); row++) {
        SshKeyInfo k = getRowItem(row);
        if (k != null && ((CheckBox) getWidget(row, 1)).getValue()) {
          sequenceNumbers.add(k.seq());
        }
      }
      if (sequenceNumbers.isEmpty()) {
        updateDeleteButton();
      } else {
        for (int seq : sequenceNumbers) {
          new RestApi("config").id("server")
              .view(Plugin.get().getPluginName(), "serviceusers").id(serviceUser)
              .view("sshkeys").id(seq).delete(new AsyncCallback<NoContent>() {
                public void onSuccess(NoContent result) {
                  for (int row = 1; row < getRowCount();) {
                    SshKeyInfo k = getRowItem(row);
                    if (k != null && sequenceNumbers.contains(k.seq())) {
                      removeRow(row);
                    } else {
                      row++;
                    }
                  }
                  if (getRowCount() == 1) {
                    display(Collections.<SshKeyInfo> emptyList());
                  } else {
                    updateDeleteButton();
                  }
                }

                @Override
                public void onFailure(Throwable caught) {
                  // never invoked
                }
              });
        }
      }
    }

    void display(List<SshKeyInfo> result) {
      if (result.isEmpty()) {
        setKeyTableVisible(false);
        showAddKeyBlock(true);
      } else {
        while (1 < getRowCount())
          removeRow(getRowCount() - 1);
        for (SshKeyInfo k : result) {
          addOneKey(k);
        }
        setKeyTableVisible(true);
        deleteKey.setEnabled(false);
      }
    }

    void addOneKey(SshKeyInfo k) {
      FlexCellFormatter fmt = getFlexCellFormatter();
      int row = getRowCount();
      insertRow(row);
      getCellFormatter().addStyleName(row, 0, "iconCell");
      getCellFormatter().addStyleName(row, 0, "leftMostCell");

      CheckBox sel = new CheckBox();
      sel.addValueChangeHandler(updateDeleteHandler);

      setWidget(row, 1, sel);
      if (k.isValid()) {
        setText(row, 2, "");
        fmt.removeStyleName(row, 2, "serviceuser-sshKeyPanelInvalid");
      } else {
        setText(row, 2, "Invalid Key");
        fmt.addStyleName(row, 2, "serviceuser-sshKeyPanelInvalid");
      }
      setText(row, 3, k.algorithm());

      CopyableLabel keyLabel = new CopyableLabel(k.sshPublicKey());
      keyLabel.setPreviewText(elide(k.encodedKey(), 40));
      setWidget(row, 4, keyLabel);

      setText(row, 5, k.comment());

      fmt.addStyleName(row, 1, "iconCell");
      fmt.addStyleName(row, 4, "serviceuser-sshKeyPanelEncodedKey");
      for (int c = 2; c <= 5; c++) {
        fmt.addStyleName(row, c, "dataCell");
      }

      setRowItem(row, k);
    }

    void updateDeleteButton() {
      boolean on = false;
      for (int row = 1; row < getRowCount(); row++) {
        CheckBox sel = (CheckBox) getWidget(row, 1);
        if (sel.getValue()) {
          on = true;
          break;
        }
      }
      deleteKey.setEnabled(on);
    }

    private SshKeyInfo getRowItem(int row) {
      return sshKeyInfos.get(row);
    }

    private void setRowItem(int row, SshKeyInfo sshKeyInfo) {
      sshKeyInfos.put(row, sshKeyInfo);
    }
  }

  static String elide(String s, int len) {
    if (s == null || s.length() < len || len <= 10) {
      return s;
    }
    return s.substring(0, len - 10) + "..." + s.substring(s.length() - 10);
  }
}

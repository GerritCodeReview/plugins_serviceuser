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

import com.google.gerrit.client.rpc.NativeMap;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import java.util.List;

public class ConfigInfo extends JavaScriptObject {
  final native String getInfoMessage() /*-{ return this.info }-*/;
  final native String getOnSuccessMessage() /*-{ return this.on_success }-*/;
  final native boolean getAllowEmail() /*-{ return this.allow_email ? true : false; }-*/;
  final native boolean getAllowHttpPassword() /*-{ return this.allow_http_password ? true : false; }-*/;
  final native boolean getAllowOwner() /*-{ return this.allow_owner ? true : false; }-*/;
  final native boolean getCreateNotes() /*-{ return this.create_notes ? true : false; }-*/;
  final native boolean getCreateNotesAsync() /*-{ return this.create_notes_async ? true : false; }-*/;
  final native JsArrayString getBlockedNames() /*-{ return this.blocked_names; }-*/;
  final native NativeMap<GroupInfo> getGroups() /*-{ return this.groups; }-*/;

  final native void setInfoMessage(String s) /*-{ this.info = s; }-*/;
  final native void setOnSuccessMessage(String s) /*-{ this.on_success = s; }-*/;
  final native void setAllowEmail(boolean s) /*-{ this.allow_email = s; }-*/;
  final native void setAllowHttpPassword(boolean s) /*-{ this.allow_http_password = s; }-*/;
  final native void setAllowOwner(boolean s) /*-{ this.allow_owner = s; }-*/;
  final native void setCreateNotes(boolean s) /*-{ this.create_notes = s; }-*/;
  final native void setCreateNotesAsync(boolean s) /*-{ this.create_notes_async = s; }-*/;

  final void setBlockedNames(List<String> blockedNames) {
    initBlockedNames();
    for (String n : blockedNames) {
      addBlockedName(n);
    }

  }
  final native void initBlockedNames() /*-{ this.blocked_names = []; }-*/;
  final native void addBlockedName(String n) /*-{ this.blocked_names.push(n); }-*/;

  final void setGroups(List<String> groups) {
    initGroups();
    for (String g : groups) {
      addGroup(g);
    }

  }
  final native void initGroups() /*-{ this.groups = []; }-*/;
  final native void addGroup(String g) /*-{ this.groups.push(g); }-*/;

  static ConfigInfo create() {
    ConfigInfo g = (ConfigInfo) createObject();
    return g;
  }

  protected ConfigInfo() {
  }
}

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

import com.google.gwt.core.client.JavaScriptObject;

public class ServiceUserInfo extends JavaScriptObject {
  public final native int _account_id() /*-{ return this._account_id || 0; }-*/;
  public final native String name() /*-{ return this.name; }-*/;
  public final native String username() /*-{ return this.username; }-*/;
  public final native String email() /*-{ return this.email; }-*/;
  public final native String created_by() /*-{ return this.created_by; }-*/;
  public final native String created_at() /*-{ return this.created_at; }-*/;

  protected ServiceUserInfo() {
  }
}

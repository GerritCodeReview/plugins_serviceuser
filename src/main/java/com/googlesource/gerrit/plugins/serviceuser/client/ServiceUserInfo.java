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

import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.GroupInfo;

public class ServiceUserInfo {
  public final String getDisplayName() {
    if (created_by.username != null) {
      return created_by.username;
    }
    if (created_by._accountId != -1) {
      return Integer.toString(created_by._accountId);
    }
    return "N/A";
  }

  public int _account_id;
  public String name;
  public String username;
  public String email;
  public AccountInfo created_by;
  public String created_at;
  public boolean active;
  public GroupInfo owner;

  protected ServiceUserInfo() {}
}

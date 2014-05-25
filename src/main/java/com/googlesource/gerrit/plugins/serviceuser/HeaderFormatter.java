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

package com.googlesource.gerrit.plugins.serviceuser;

import com.google.common.base.Strings;
import com.google.gerrit.server.account.AccountInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class HeaderFormatter {
  private final StringBuilder sb = new StringBuilder();
  private final DateFormat rfc2822DateFormatter;
  private final String anonymousCowardName;

  HeaderFormatter(TimeZone tz, String anonymousCowardName) {
    this.anonymousCowardName = anonymousCowardName;
    rfc2822DateFormatter =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
    rfc2822DateFormatter.setCalendar(Calendar.getInstance(tz, Locale.US));
  }

  void append(String key, String value) {
    sb.append(key).append(": ").append(value).append("\n");
  }

  void appendDate() {
    sb.append("Date: ").append(rfc2822DateFormatter.format(new Date()))
        .append("\n");
  }

  void appendUser(String key, AccountInfo user) {
    sb.append(key);
    sb.append(": ");
    appendUserData(user);
    sb.append("\n");
  }

  private void appendUserData(AccountInfo user) {
    boolean needSpace = false;
    boolean wroteData = false;

    if (!Strings.isNullOrEmpty(user.name)) {
      sb.append(user.name);
      needSpace = true;
      wroteData = true;
    }

    if (!Strings.isNullOrEmpty(user.email)) {
      if (needSpace) {
        sb.append(" ");
      }
      sb.append("<").append(user.email).append(">");
      wroteData = true;
    }

    if (!wroteData) {
      sb.append(anonymousCowardName).append(" #").append(user._id.get());
    }
  }

  @Override
  public String toString() {
    return sb.toString();
  }
}

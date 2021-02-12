// Copyright (C) 2021 The Android Open Source Project
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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Singleton
public class BlockedNameFilter {
  private final PluginConfig cfg;
  private final List<String> blockedExactNames = new ArrayList<>();
  private final List<String> blockedNamePrefixes = new ArrayList<>();
  private final List<Pattern> blockedRegexNames = new ArrayList<>();

  @Inject
  public BlockedNameFilter(PluginConfigFactory cfgFactory, @PluginName String pluginName) {
    this.cfg = cfgFactory.getFromGerritConfig(pluginName);
    parseConfig();
  }

  public boolean isBlocked(String username) {
    username = username.toLowerCase();
    return isBlockedByExactName(username)
        || isBlockedByWildcard(username)
        || isBlockedByRegex(username);
  }

  private void parseConfig() {
    for (String s : cfg.getStringList("block")) {
      if (s.startsWith("^")) {
        blockedRegexNames.add(Pattern.compile(s, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
      } else if (s.endsWith("*")) {
        blockedNamePrefixes.add(s.substring(0, s.length() - 1).toLowerCase());
      } else {
        blockedExactNames.add(s.toLowerCase());
      }
    }
  }

  private boolean isBlockedByExactName(String username) {
    return blockedExactNames.contains(username);
  }

  private boolean isBlockedByWildcard(String username) {
    for (String prefix : blockedNamePrefixes) {
      if (username.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private boolean isBlockedByRegex(String username) {
    for (Pattern p : blockedRegexNames) {
      if (p.matcher(username).find()) {
        return true;
      }
    }
    return false;
  }
}

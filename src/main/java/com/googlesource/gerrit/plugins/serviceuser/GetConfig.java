// Copyright (C) 2013 The Android Open Source Project
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
import com.google.gerrit.common.data.GroupDescriptions;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.group.GroupJson;
import com.google.gerrit.server.group.GroupJson.GroupInfo;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GetConfig implements RestReadView<ConfigResource> {
  private static final Logger log = LoggerFactory.getLogger(GetConfig.class);

  private final PluginConfig cfg;
  private final GroupCache groupCache;
  private final GroupJson groupJson;

  @Inject
  public GetConfig(PluginConfigFactory cfgFactory,
      @PluginName String pluginName, GroupCache groupCache, GroupJson groupJson) {
    this.cfg = cfgFactory.getFromGerritConfig(pluginName);
    this.groupCache = groupCache;
    this.groupJson = groupJson;
  }

  @Override
  public ConfigInfo apply(ConfigResource rsrc) throws OrmException {
    ConfigInfo info = new ConfigInfo();
    info.info = Strings.emptyToNull(cfg.getString("infoMessage"));
    info.onSuccess = Strings.emptyToNull(cfg.getString("onSuccessMessage"));
    info.allowEmail = toBoolean(cfg.getBoolean("allowEmail", false));
    info.allowOwner = toBoolean(cfg.getBoolean("allowOwner", false));
    info.createNotes = toBoolean(cfg.getBoolean("createNotes", true));
    info.createNotesAsync = toBoolean(cfg.getBoolean("createNotesAsync", false));

    String[] blocked = cfg.getStringList("block");
    Arrays.sort(blocked);
    info.blockedNames = Arrays.asList(blocked);

    String[] groups = cfg.getStringList("group");
    info.groups = new TreeMap<>();
    for (String g : groups) {
      AccountGroup group = groupCache.get(new AccountGroup.NameKey(g));
      if (group != null) {
        GroupInfo groupInfo = groupJson.format(GroupDescriptions.forAccountGroup(group));
        groupInfo.name = null;
        info.groups.put(g, groupInfo);
      } else {
        log.warn(String.format("Service user group %s does not exist.", g));
      }
    }

    return info;
  }

  private static Boolean toBoolean(boolean v) {
    return v ? v : null;
  }

  public class ConfigInfo {
    public String info;
    public String onSuccess;
    public Boolean allowEmail;
    public Boolean allowOwner;
    public Boolean createNotes;
    public Boolean createNotesAsync;
    public List<String> blockedNames;
    public Map<String, GroupInfo> groups;
  }
}

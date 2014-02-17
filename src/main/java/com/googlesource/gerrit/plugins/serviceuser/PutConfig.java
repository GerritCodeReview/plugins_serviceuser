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
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.serviceuser.PutConfig.Input;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

import java.io.IOException;

@RequiresCapability(GlobalCapability.ADMINISTRATE_SERVER)
public class PutConfig implements RestModifyView<ConfigResource, Input>{
  public static class Input {
    public String info;
    public String onSuccess;
    public Boolean allowEmail;
    public Boolean createNotes;
    public Boolean createNotesAsync;
  }

  private final PluginConfigFactory cfgFactory;
  private final SitePaths sitePaths;
  private final String pluginName;

  @Inject
  PutConfig(PluginConfigFactory cfgFactory, SitePaths sitePaths,
      @PluginName String pluginName) throws IOException, ConfigInvalidException {
    this.cfgFactory = cfgFactory;
    this.sitePaths = sitePaths;
    this.pluginName = pluginName;
  }

  @Override
  public Response<String> apply(ConfigResource rsrc, Input input)
      throws IOException, ConfigInvalidException {
    FileBasedConfig cfg =
        new FileBasedConfig(sitePaths.gerrit_config, FS.DETECTED);
    cfg.load();
    if (input.info != null) {
      cfg.setString("plugin", pluginName, "infoMessage",
          Strings.emptyToNull(input.info));
    }
    if (input.onSuccess != null) {
      cfg.setString("plugin", pluginName, "onSuccessMessage",
          Strings.emptyToNull(input.onSuccess));
    }
    if (input.allowEmail != null) {
      setBoolean(cfg, "allowEmail", input.allowEmail);
    }
    if (input.createNotes != null) {
      setBoolean(cfg, "createNotes", input.createNotes, true);
    }
    if (input.createNotesAsync != null) {
      setBoolean(cfg, "createNotesAsync", input.createNotesAsync);
    }
    cfg.save();
    cfgFactory.getFromGerritConfig(pluginName, true);
    return Response.<String> ok("OK");
  }

  private void setBoolean(Config cfg, String name, boolean value) {
    setBoolean(cfg, name, value, false);
  }

  private void setBoolean(Config cfg, String name, boolean value, boolean defaultValue) {
    if (value == defaultValue) {
      cfg.unset("plugin", pluginName, name);
    } else {
      cfg.setBoolean("plugin", pluginName, name, value);
    }
  }
}

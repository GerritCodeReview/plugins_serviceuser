// Copyright (C) 2025 The Android Open Source Project
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

import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.UseSsh;
import com.google.gerrit.acceptance.config.GerritConfig;

import org.junit.Before;
import org.junit.Test;

@UseSsh
@TestPlugin(
    name = "serviceuser",
    sysModule = "com.googlesource.gerrit.plugins.serviceuser.Module",
    sshModule = "com.googlesource.gerrit.plugins.serviceuser.SshModule",
    httpModule = "com.googlesource.gerrit.plugins.serviceuser.HttpModule")
public class DeleteTokenIT extends LightweightPluginDaemonTest {
  private static final String SERVICEUSER_NAME = "testServiceuser";
  private static final String OWNER_GROUP_NAME = "testGroup";
  private static final String SERVICEUSER_TOKEN_ID = "token";
  private static final String SERVICEUSER_BASE_URL = "/config/server/serviceuser~serviceusers/";

  @Before
  public void setUp() throws Exception {
	  adminRestSession.put(SERVICEUSER_BASE_URL + SERVICEUSER_NAME).assertCreated();
	  adminRestSession.put("/groups/" + OWNER_GROUP_NAME).assertCreated();
	  
	  PutOwner.Input ownerInput = new PutOwner.Input();
	  ownerInput.group = OWNER_GROUP_NAME;
	  adminRestSession.put(SERVICEUSER_BASE_URL + SERVICEUSER_NAME + "/owner", ownerInput).assertCreated();
	  
	  adminRestSession.put(SERVICEUSER_BASE_URL + SERVICEUSER_NAME + "/tokens/" + SERVICEUSER_TOKEN_ID).assertCreated();
  }

  @Test
  @GerritConfig(name = "plugin.serviceuser.allowHttpPassword", value = "true")
  public void testDeleteToken() throws Exception {
    userRestSession.delete(SERVICEUSER_BASE_URL + SERVICEUSER_NAME + "/tokens/" + SERVICEUSER_TOKEN_ID).assertNotFound();
    adminRestSession.put("/groups/" + OWNER_GROUP_NAME + "/members/" + user.id());
    userRestSession.delete(SERVICEUSER_BASE_URL + SERVICEUSER_NAME + "/tokens/" + SERVICEUSER_TOKEN_ID).assertNoContent();
  }
}

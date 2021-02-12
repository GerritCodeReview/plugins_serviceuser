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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BlockedNameFilterTest {

  private static String[] BLOCKED_NAMES =
      new String[] {"exact", "ex*act", "wild*", "^regex[0-9]+", "regex[0-9]+", "^[導字會]+"};

  private BlockedNameFilter blockedNameFilter;

  @Mock private PluginConfigFactory configFactory;

  @Mock private PluginConfig config;

  @Before
  public void setup() {
    when(configFactory.getFromGerritConfig("serviceuser")).thenReturn(config);
    when(config.getStringList("block")).thenReturn(BLOCKED_NAMES);
    blockedNameFilter = new BlockedNameFilter(configFactory, "serviceuser");
  }

  @Test
  public void exactMatchIsBlocked() {
    assertTrue(blockedNameFilter.apply("exact"));
    assertTrue(blockedNameFilter.apply("ExAct"));
    assertTrue(blockedNameFilter.apply("ex*act"));
    assertTrue(blockedNameFilter.apply("regex[0-9]+"));
    assertFalse(blockedNameFilter.apply("notexact"));
    assertFalse(blockedNameFilter.apply("exxact"));
  }

  @Test
  public void wildcardMatchIsBlocked() {
    assertTrue(blockedNameFilter.apply("wild"));
    assertTrue(blockedNameFilter.apply("wildcard"));
    assertTrue(blockedNameFilter.apply("Wilde"));
    assertFalse(blockedNameFilter.apply("wil"));
  }

  @Test
  public void regexMatchIsBlocked() {
    assertTrue(blockedNameFilter.apply("regex1"));
    assertTrue(blockedNameFilter.apply("Regex1"));
    assertTrue(blockedNameFilter.apply("導"));
  }
}

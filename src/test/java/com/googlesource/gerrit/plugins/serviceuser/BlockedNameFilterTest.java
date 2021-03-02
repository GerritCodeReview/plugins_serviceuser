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

import static com.google.common.truth.Truth.assertThat;
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
      new String[] {
        "exact", "ex*act", "wild*", "^regex[0-9]+", "^ABC", "^[0-9]+$", "regex[0-9]+", "^⁋+"
      };

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
    assertThat(blockedNameFilter.isBlocked("exact")).isTrue();
    assertThat(blockedNameFilter.isBlocked("ExAct")).isTrue();
    assertThat(blockedNameFilter.isBlocked("ex*act")).isTrue();
    assertThat(blockedNameFilter.isBlocked("regex[0-9]+")).isTrue();
    assertThat(blockedNameFilter.isBlocked("notexact")).isFalse();
    assertThat(blockedNameFilter.isBlocked("exxact")).isFalse();
  }

  @Test
  public void wildcardMatchIsBlocked() {
    assertThat(blockedNameFilter.isBlocked("wild")).isTrue();
    assertThat(blockedNameFilter.isBlocked("wildcard")).isTrue();
    assertThat(blockedNameFilter.isBlocked("Wilde")).isTrue();
    assertThat(blockedNameFilter.isBlocked("wil")).isFalse();
  }

  @Test
  public void regexMatchIsBlocked() {
    assertThat(blockedNameFilter.isBlocked("regex1")).isTrue();
    assertThat(blockedNameFilter.isBlocked("Regex1")).isTrue();

    // Pattern matching is done at the beginning of the username
    assertThat(blockedNameFilter.isBlocked("foo-regex1")).isFalse();

    // Names with unicode characters can be blocked
    assertThat(blockedNameFilter.isBlocked("⁋")).isTrue();

    // Regex matches only complete name, when ending with '$'.
    assertThat(blockedNameFilter.isBlocked("01234")).isTrue();
    assertThat(blockedNameFilter.isBlocked("01234abcd")).isFalse();

    // Regex matches prefix without trailing '$'
    assertThat(blockedNameFilter.isBlocked("regex1suffix")).isTrue();

    // Uppercase regex matches case-insenstive
    assertThat(blockedNameFilter.isBlocked("abc")).isTrue();
    assertThat(blockedNameFilter.isBlocked("ABC")).isTrue();
  }
}

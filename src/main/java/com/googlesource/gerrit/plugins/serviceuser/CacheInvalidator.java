// Copyright (C) 2024 The Android Open Source Project
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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.inject.Inject;

class CacheInvalidator implements EventListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final AllProjectsName allProjects;
  private final StorageCache storageCache;

  @Inject
  CacheInvalidator(AllProjectsName allProjects, StorageCache storageCache) {
    this.allProjects = allProjects;
    this.storageCache = storageCache;
  }

  @Override
  public void onEvent(Event event) {
    // This is needed in a multi-site setup to make sure every Gerrit instance
    // has the latest created serviceuser in cache.
    if (event.getType().equals(RefUpdatedEvent.TYPE)) {
      RefUpdatedEvent refUpdatedEvent = (RefUpdatedEvent) event;
      if (refUpdatedEvent.getProjectNameKey().get().equals(allProjects.get())
          && refUpdatedEvent.getRefName().equals(RefNames.REFS_CONFIG)) {
        logger.atFine().log("All-Projects ref update triggered, invalidate serviceuser cache");
        storageCache.invalidate();
      }
    }
  }
}

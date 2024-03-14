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

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.cache.CacheModule;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.git.meta.MetaDataUpdate;
import com.google.gerrit.server.git.meta.VersionedConfigFile;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutionException;
import org.eclipse.jgit.lib.Config;

@Singleton
public class StorageCache {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String CACHE_NAME = "storage";
  private static final Object ALL = new Object();

  static CacheModule module() {
    return new CacheModule() {
      @Override
      protected void configure() {
        cache(CACHE_NAME, Object.class, Config.class).loader(Loader.class);
        bind(StorageCache.class);
      }
    };
  }

  private final LoadingCache<Object, Config> cache;

  @Inject
  StorageCache(@Named(CACHE_NAME) LoadingCache<Object, Config> cache) {
    this.cache = cache;
  }

  public Config get() {
    try {
      return cache.get(ALL);
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e).log("Cannot load service users");
      return new Config();
    }
  }

  public void invalidate() {
    cache.invalidate(ALL);
  }

  static class Loader extends CacheLoader<Object, Config> {
    private final Provider<VersionedConfigFile> configProvider;
    private final MetaDataUpdate.Server metaDataUpdateFactory;
    private final AllProjectsName allProjects;

    @Inject
    Loader(
        Provider<VersionedConfigFile> configProvider,
        MetaDataUpdate.Server metaDataUpdateFactory,
        AllProjectsName allProjects) {
      this.configProvider = configProvider;
      this.metaDataUpdateFactory = metaDataUpdateFactory;
      this.allProjects = allProjects;
    }

    @Override
    public Config load(Object key) throws Exception {
      VersionedConfigFile storage = configProvider.get();
      try (MetaDataUpdate md = metaDataUpdateFactory.create(allProjects)) {
        storage.load(md);
      }
      return storage.getConfig();
    }
  }
}

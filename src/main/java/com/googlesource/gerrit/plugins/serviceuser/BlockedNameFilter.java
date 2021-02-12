
package com.googlesource.gerrit.plugins.serviceuser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class BlockedNameFilter {
  private final PluginConfig cfg;
  private final List<String> blockedExactNames = new ArrayList<>();
  private final List<Pattern> blockedRegexNames = new ArrayList<>();
  private final List<String> blockedNamePrefixes = new ArrayList<>();

  @Inject
  public BlockedNameFilter(
      PluginConfigFactory cfgFactory,
      @PluginName String pluginName) {
    this.cfg = cfgFactory.getFromGerritConfig(pluginName);
    parseConfig();
  }
  
  public boolean apply(String username) {
    if (filterExactName(username)) {
      return true;
    }
    
    if (filterWildcardName(username)) {
      return true;
    }
    
    if (filterRegexName(username)) {
      return true;
    }
    
    return false;
  }
  
  private void parseConfig() {
    List<String> blockedNames = Arrays.asList(cfg.getStringList("block"));
    for (String s: blockedNames) {
      if (s.startsWith("^")) {
        blockedRegexNames.add(Pattern.compile(s));
      } else if (s.endsWith("*")) {
        blockedNamePrefixes.add(s.substring(0, s.length()-1).toLowerCase());
      } else {
        blockedExactNames.add(s.toLowerCase());
      }
    }
  }
  
  private boolean filterExactName(String username) {
    return this.blockedExactNames.contains(username);
  }
  
  private boolean filterWildcardName(String username) {
    for (String prefix: this.blockedNamePrefixes) {
      if (username.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
  
  private boolean filterRegexName(String username) {
    for (Pattern p: this.blockedRegexNames) {
      if (p.matcher(username).find()) {
        return true;
      }
    }
    return false;
  }
}

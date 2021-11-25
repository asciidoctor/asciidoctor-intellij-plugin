package org.asciidoc.intellij.psi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache a list of section for a given key.
 */
public class ProjectSectionCache {
  private final Map<String, List<AsciiDocSection>> cache = new ConcurrentHashMap<>();

  public List<AsciiDocSection> get(String key) {
    return cache.get(key);
  }

  public void put(String key, List<AsciiDocSection> value) {
    cache.put(key, value);
  }

}

package org.asciidoc.intellij.psi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache a list of block ids for a given key.
 */
public class ProjectBlockidCache {
  private final Map<String, List<AsciiDocBlockId>> cache = new ConcurrentHashMap<>();

  public List<AsciiDocBlockId> get(String key) {
    return cache.get(key);
  }

  public void put(String key, List<AsciiDocBlockId> value) {
    cache.put(key, value);
  }

}

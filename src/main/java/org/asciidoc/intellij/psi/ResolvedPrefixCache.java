package org.asciidoc.intellij.psi;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache a list of resolved prefixes.
 */
public class ResolvedPrefixCache {
  private final Map<Key, List<VirtualFile>> cache = new ConcurrentHashMap<>();

  public static class Key {

    private final VirtualFile moduleDir;
    private final String otherKey;

    public Key(VirtualFile moduleDir, String otherKey) {
      this.moduleDir = moduleDir;
      this.otherKey = otherKey;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Key)) {
        return false;
      }
      Key key = (Key) o;
      return Objects.equals(moduleDir, key.moduleDir) && Objects.equals(otherKey, key.otherKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(moduleDir, otherKey);
    }
  }

  public List<VirtualFile> get(VirtualFile moduleDir, String otherKey) {
    return cache.get(new Key(moduleDir, otherKey));
  }

  public void put(VirtualFile moduleDir, String otherKey, List<VirtualFile> value) {
    cache.put(new Key(moduleDir, otherKey), value);
  }

}

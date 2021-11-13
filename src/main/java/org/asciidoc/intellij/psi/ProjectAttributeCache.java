package org.asciidoc.intellij.psi;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache a list of attributes for a given key and scope.
 */
public class ProjectAttributeCache {
  private final Map<Key, List<AttributeDeclaration>> cache = new ConcurrentHashMap<>();

  public static class Key {
    private final String key;
    private final boolean onlyAntora;

    public Key(String key, boolean onlyAntora) {
      this.key = key;
      this.onlyAntora = onlyAntora;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Key)) {
        return false;
      }
      Key key1 = (Key) o;
      return Objects.equals(key, key1.key) && onlyAntora == key1.onlyAntora;
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, onlyAntora);
    }
  }

  public List<AttributeDeclaration> get(String key, boolean onlyAntora) {
    return cache.get(new Key(key, onlyAntora));
  }

  public void put(String key, boolean onlyAntora, List<AttributeDeclaration> value) {
    cache.put(new Key(key, onlyAntora), value);
  }

}

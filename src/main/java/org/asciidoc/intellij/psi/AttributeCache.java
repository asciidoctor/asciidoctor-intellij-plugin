package org.asciidoc.intellij.psi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cache a list of attributes for a given key and scope.
 */
public class AttributeCache {
  private static final String PAGE_ATTRIBUTES = "!page_attributes";
  private final Map<Key, List<AttributeDeclaration>> cache = new HashMap<>();

  public static class Key {
    private final String key;
    private final AsciiDocUtil.Scope scope;

    public Key(String key, AsciiDocUtil.Scope scope) {
      this.key = key;
      this.scope = scope;
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
      return Objects.equals(key, key1.key) && scope == key1.scope;
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, scope);
    }
  }

  public synchronized List<AttributeDeclaration> get(String key, AsciiDocUtil.Scope scope) {
    return cache.get(new Key(key, scope));
  }

  public List<AttributeDeclaration> getPageAttributes() {
    return get(PAGE_ATTRIBUTES, null);
  }

  public synchronized void put(String key, AsciiDocUtil.Scope scope, List<AttributeDeclaration> value) {
    cache.put(new Key(key, scope), value);
  }

  public void putPageAttributes(List<AttributeDeclaration> value) {
    put(PAGE_ATTRIBUTES, null, value);
  }

}

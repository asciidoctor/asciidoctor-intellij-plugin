package org.asciidoc.intellij.psi;

import java.util.Locale;

public interface AttributeDeclaration {
  String getAttributeValue();

  String getAttributeName();

  boolean isSoft();

  default boolean matchesKey(String key) {
    return getAttributeName().toLowerCase(Locale.US).equals(key.toLowerCase(Locale.US));
  }
}

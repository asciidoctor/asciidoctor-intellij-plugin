package org.asciidoc.intellij.psi;

import java.util.Objects;

public class AsciiDocAttributeDeclarationDummy implements AttributeDeclaration {
  private final String attributeValue, attributeName;

  public AsciiDocAttributeDeclarationDummy(String attributeName, String attributeValue) {
    this.attributeValue = attributeValue;
    this.attributeName = attributeName;
  }

  @Override
  public String getAttributeValue() {
    return attributeValue;
  }

  @Override
  public String getAttributeName() {
    return attributeName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AsciiDocAttributeDeclarationDummy)) {
      return false;
    }
    AsciiDocAttributeDeclarationDummy that = (AsciiDocAttributeDeclarationDummy) o;
    return Objects.equals(attributeValue, that.attributeValue) &&
      Objects.equals(attributeName, that.attributeName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributeValue, attributeName);
  }

}

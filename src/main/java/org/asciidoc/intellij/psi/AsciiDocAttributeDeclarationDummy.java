package org.asciidoc.intellij.psi;

import java.util.Objects;

public class AsciiDocAttributeDeclarationDummy implements AttributeDeclaration {
  private final String attributeValue, attributeName;
  private final boolean soft;

  public AsciiDocAttributeDeclarationDummy(String attributeName, String attributeValue) {
    this(attributeName, attributeValue, attributeValue == null);
  }

  public AsciiDocAttributeDeclarationDummy(String attributeName, String attributeValue, boolean soft) {
    if (attributeValue != null) {
      if (attributeName.endsWith("@")) {
        soft = true;
        attributeName = attributeName.substring(0, attributeName.length() - 1);
      } else if (attributeValue.endsWith("@")) {
        soft = true;
        attributeValue = attributeValue.substring(0, attributeValue.length() - 1);
      }
    }
    this.attributeValue = attributeValue;
    this.attributeName = attributeName;
    this.soft = soft;
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
  public boolean isSoft() {
    return soft;
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
      Objects.equals(attributeName, that.attributeName) &&
      isSoft() == that.isSoft();
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributeValue, attributeName);
  }

}

package org.asciidoc.intellij.psi;

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
}

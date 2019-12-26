package org.asciidoc.intellij.psi;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;

public class AsciiDocAttributeDeclarationStubImpl extends StubBase<AsciiDocAttributeDeclarationImpl> implements AsciiDocAttributeDeclarationStub {
  private final String attributeName;
  private final String attributeValue;

  public AsciiDocAttributeDeclarationStubImpl(StubElement parent, String attributeName, String attributeValue) {
    super(parent, AsciiDocElementTypes.ATTRIBUTE_DECLARATION);
    this.attributeName = attributeName;
    this.attributeValue = attributeValue;
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

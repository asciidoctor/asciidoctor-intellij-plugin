package org.asciidoc.intellij.psi;

import com.intellij.psi.stubs.StubElement;

public interface AsciiDocAttributeDeclarationStub extends StubElement<AsciiDocAttributeDeclarationImpl>  {
  String getAttributeValue();

  String getAttributeName();
}

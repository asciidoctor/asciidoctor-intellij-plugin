package org.asciidoc.intellij.psi;

import com.intellij.psi.StubBasedPsiElement;

public interface AsciiDocAttributeDeclaration extends StubBasedPsiElement<AsciiDocAttributeDeclarationStub>, AttributeDeclaration {
  AsciiDocAttributeDeclarationName getAttributeDeclarationName();
}

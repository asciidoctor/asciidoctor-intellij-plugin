package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocAttributeDeclaration extends ASTWrapperPsiElement {
  public AsciiDocAttributeDeclaration(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  public String getAttributeName() {
    AsciiDocAttributeDeclarationName attributeName = findChildByType(AsciiDocElementTypes.ATTRIBUTE_DECLARATION_NAME);
    if (attributeName != null) {
      return attributeName.getName();
    }
    return null;
  }

  public String getAttributeValue() {
    ASTNode attributeValue = getNode().findChildByType(AsciiDocTokenTypes.ATTRIBUTE_VAL);
    if (attributeValue != null) {
      return attributeValue.getText().trim();
    }
    return null;
  }

  @NotNull
  @Override
  public PsiElement getNavigationElement() {
    AsciiDocAttributeDeclarationName attributeName = findChildByType(AsciiDocElementTypes.ATTRIBUTE_DECLARATION_NAME);
    if (attributeName != null) {
      return attributeName;
    }
    return this;
  }

}

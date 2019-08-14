package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  private static final TokenSet CONTINUATION_TYPES = TokenSet.create(AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION,
    AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION_LEGACY);

  @Nullable
  public String getAttributeValue() {
    ASTNode attributeValue = getNode().findChildByType(AsciiDocTokenTypes.ATTRIBUTE_VAL);
    if (attributeValue != null) {
      StringBuilder sb = new StringBuilder();
      while (attributeValue != null) {
        if (CONTINUATION_TYPES.contains(attributeValue.getElementType())) {
          sb.append(" ");
        } else {
          sb.append(attributeValue.getText());
        }
        attributeValue = attributeValue.getTreeNext();
      }
      String val = sb.toString().trim();
      if (val.contains("{asciidoctorconfigdir}") &&
        (this.getContainingFile().getName().equals(".asciidoctorconfig") || this.getContainingFile().getName().equals(".asciidoctorconfig.adoc"))
        && this.getContainingFile().getVirtualFile().getParent().getCanonicalPath() != null) {
        val = val.replaceAll("\\{asciidoctorconfigdir}", this.getContainingFile().getVirtualFile().getParent().getCanonicalPath());
      }
      return val;
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

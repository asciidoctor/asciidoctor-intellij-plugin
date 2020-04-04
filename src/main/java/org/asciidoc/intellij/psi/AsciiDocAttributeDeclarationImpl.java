package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocAttributeDeclarationImpl
        extends AsciiDocAttributeDeclarationStubElementImpl<AsciiDocAttributeDeclarationStub>
        implements AsciiDocAttributeDeclaration {

  public AsciiDocAttributeDeclarationImpl(AsciiDocAttributeDeclarationStub stub, IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public AsciiDocAttributeDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  @Override
  public String getAttributeName() {
    AsciiDocAttributeDeclarationStub greenStub = getGreenStub();
    if (greenStub != null) {
      return greenStub.getAttributeName();
    }
    AsciiDocAttributeDeclarationName attributeName = findChildByType(AsciiDocElementTypes.ATTRIBUTE_DECLARATION_NAME);
    if (attributeName != null) {
      return attributeName.getName();
    }
    return null;
  }

  /**
   * Check if it is an unset attribute declaration (':attr!:').
   */
  public boolean isUnset() {
    return this.getNode().getChildren(TokenSet.create(AsciiDocTokenTypes.ATTRIBUTE_UNSET)).length > 0;
  }

  private static final TokenSet CONTINUATION_TYPES = TokenSet.create(AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION,
    AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION_LEGACY);

  @Nullable
  @Override
  public String getAttributeValue() {
    AsciiDocAttributeDeclarationStub greenStub = getGreenStub();
    if (greenStub != null) {
      String value = greenStub.getAttributeValue();
      // asciidoctorconfigdir might not be properly replaced during indexing, therefore don't use stub value
      if (value == null || !value.contains("{asciidoctorconfigdir}")) {
        return value;
      }
    }
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
        && this.getContainingFile().getVirtualFile() != null && this.getContainingFile().getVirtualFile().getParent() != null
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
    AsciiDocAttributeDeclarationName attributeName = getAttributeDeclarationName();
    if (attributeName != null) {
      return attributeName;
    }
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + getNode().getElementType().toString() + ")";
  }

  @Override
  public AsciiDocAttributeDeclarationName getAttributeDeclarationName() {
    return findChildByType(AsciiDocElementTypes.ATTRIBUTE_DECLARATION_NAME);
  }
}

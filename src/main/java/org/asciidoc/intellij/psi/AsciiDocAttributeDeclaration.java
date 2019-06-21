package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
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
    ASTNode attributeName = getNode().findChildByType(AsciiDocTokenTypes.ATTRIBUTE_NAME);
    if (attributeName != null) {
      return attributeName.getText().trim();
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

  public static class Manipulator extends AbstractElementManipulator<AsciiDocAttributeDeclaration> {

    @Override
    public AsciiDocAttributeDeclaration handleContentChange(@NotNull AsciiDocAttributeDeclaration element,
      @NotNull TextRange range,
      String newContent) throws IncorrectOperationException {
      PsiElement child = element.getFirstChild();
      while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ATTRIBUTE_NAME) {
        range = range.shiftRight(-child.getTextLength());
        child = child.getNextSibling();
      }
      if (child instanceof LeafPsiElement) {
        ((LeafPsiElement) child).replaceWithText(range.replace(child.getText(), newContent));
      } else {
        throw new IncorrectOperationException("Bad child");
      }

      return element;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull AsciiDocAttributeDeclaration element) {
      PsiElement child = element.findChildByType(AsciiDocTokenTypes.ATTRIBUTE_NAME);
      if (child != null) {
        return TextRange.create(child.getStartOffsetInParent(), child.getStartOffsetInParent() + child.getTextLength());
      } else {
        return TextRange.EMPTY_RANGE;
      }
    }
  }
}

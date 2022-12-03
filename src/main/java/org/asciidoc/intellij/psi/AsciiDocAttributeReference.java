package org.asciidoc.intellij.psi;

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

public class AsciiDocAttributeReference extends AsciiDocASTWrapperPsiElement {

  @Override
  public String getName() {
    return getRangeOfBody(this).substring(this.getText());
  }

  public AsciiDocAttributeReference(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocAttributeReference> {

    @Override
    public AsciiDocAttributeReference handleContentChange(@NotNull AsciiDocAttributeReference element,
      @NotNull TextRange range,
      String newContent) throws IncorrectOperationException {
      PsiElement child = element.getFirstChild();
      while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ATTRIBUTE_REF) {
        range = range.shiftRight(-child.getTextLength());
        child = child.getNextSibling();
      }
      if (child instanceof LeafPsiElement && range.getEndOffset() <= child.getTextLength()) {
        ((LeafPsiElement) child).replaceWithText(range.replace(child.getText(), newContent));
      } else {
        AsciiDocPsiImplUtil.throwExceptionCantHandleContentChange(element, range, newContent);
      }

      return element;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull AsciiDocAttributeReference element) {
      return AsciiDocAttributeReference.getRangeOfBody(element);
    }
  }

  private static TextRange getRangeOfBody(AsciiDocAttributeReference element) {
    PsiElement child = element.findChildByType(AsciiDocTokenTypes.ATTRIBUTE_REF);
    if (child != null) {
      return TextRange.create(child.getStartOffsetInParent(), child.getStartOffsetInParent() + child.getTextLength());
    } else {
      return TextRange.EMPTY_RANGE;
    }
  }
}

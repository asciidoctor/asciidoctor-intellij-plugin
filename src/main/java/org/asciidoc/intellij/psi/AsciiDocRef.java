package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AsciiDocRef extends ASTWrapperPsiElement implements HasAnchorReference, HasFileReference {
  public AsciiDocRef(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    TextRange range = getRangeOfBody(this);
    if (!range.isEmpty()) {
      String file = this.getText().substring(range.getStartOffset(), range.getEndOffset());
      ArrayList<PsiReference> references = new ArrayList<>();
      int start = 0;
      int i = 0;
      for (; i < file.length(); ++i) {
        if (file.charAt(i) == '/' || file.charAt(i) == '#') {
          references.add(
            new AsciiDocFileReference(this, "<<", file.substring(0, start),
              TextRange.create(range.getStartOffset() + start, range.getStartOffset() + i),
              true, false)
          );
          start = i + 1;
        }
      }
      references.add(
        new AsciiDocFileReference(this, "<<", file.substring(0, start),
          TextRange.create(range.getStartOffset() + start, range.getStartOffset() + file.length()),
          false, false)
          .withAnchor(start == 0 || file.charAt(start - 1) == '#')
      );
      return references.toArray(new PsiReference[0]);
    }
    return super.getReferences();
  }

  private static TextRange getRangeOfBody(AsciiDocRef element) {
    PsiElement child = element.getFirstChild();
    // skip over start ID
    while (child != null && child.getNode().getElementType() == AsciiDocTokenTypes.REFSTART) {
      child = child.getNextSibling();
    }
    if (child == null) {
      return TextRange.EMPTY_RANGE;
    }
    int start = child.getStartOffsetInParent();
    int end = start;
    while (child != null
      && child.getNode().getElementType() != AsciiDocTokenTypes.SEPARATOR
      && child.getNode().getElementType() != AsciiDocTokenTypes.REFEND) {
      end = child.getStartOffsetInParent() + child.getTextLength();
      child = child.getNextSibling();
    }
    return TextRange.create(start, end);
  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocRef> {

    @Override
    public AsciiDocRef handleContentChange(@NotNull AsciiDocRef element,
                                           @NotNull TextRange range,
                                           String newContent) throws IncorrectOperationException {
      PsiElement child = element.getFirstChild();
      while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.REF) {
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
    public TextRange getRangeInElement(@NotNull AsciiDocRef element) {
      PsiElement child = element.findChildByType(AsciiDocTokenTypes.REF);
      if (child != null) {
        return TextRange.create(child.getStartOffsetInParent(), child.getStartOffsetInParent() + child.getTextLength());
      } else {
        return TextRange.EMPTY_RANGE;
      }
    }
  }
}

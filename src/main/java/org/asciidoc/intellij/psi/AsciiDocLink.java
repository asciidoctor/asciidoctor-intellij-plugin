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
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocLink extends ASTWrapperPsiElement {
  public AsciiDocLink(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocLink> {

    /**
     * This method will be called when either the anchor or the file is renamed.
     * It will find the appropriate child element and rename it.
     * <p>
     * Limitation: If the content change would spread two children, an out-of-bounds-exception would occur.
     * Situation when this would happen is unclear, therefore not implemented for now.
     */
    @Override
    public AsciiDocLink handleContentChange(@NotNull AsciiDocLink element,
                                            @NotNull TextRange range,
                                            String newContent) throws IncorrectOperationException {
      PsiElement child = element.getFirstChild();
      while (child != null && range.getStartOffset() >= child.getTextLength()) {
        range = range.shiftRight(-child.getTextLength());
        child = child.getNextSibling();
      }
      if (child instanceof LeafPsiElement) {
        // if the old file name didn't have a .adoc suffic, the new shouldn't have one as well
        if (((LeafPsiElement) child).getElementType().equals(AsciiDocTokenTypes.LINKFILE)
          && !AsciiDocFileType.hasAsciiDocExtension(child.getText())
          && AsciiDocFileType.hasAsciiDocExtension(newContent)) {
          newContent = AsciiDocFileType.removeAsciiDocExtension(newContent);
        }
        ((LeafPsiElement) child).replaceWithText(range.replace(child.getText(), newContent));
      } else {
        throw new IncorrectOperationException("Bad child");
      }

      return element;
    }

    /**
     * The relevant text range is both link file and anchor (if present).
     * Return the start of the first element and the end of the last element.
     */
    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull AsciiDocLink element) {
      PsiElement child = element.getFirstChild();
      int startOffset = -1;
      int endOffset = -1;
      while (child != null) {
        if (child.getNode().getElementType().equals(AsciiDocTokenTypes.LINKFILE)
          || child.getNode().getElementType().equals(AsciiDocTokenTypes.LINKANCHOR)) {
          if (startOffset == -1) {
            startOffset = child.getStartOffsetInParent();
          }
          endOffset = child.getStartOffsetInParent() + child.getTextLength();
        }
        child = child.getNextSibling();
      }
      if (startOffset != -1) {
        return TextRange.create(startOffset, endOffset);
      } else {
        return TextRange.EMPTY_RANGE;
      }
    }
  }
}

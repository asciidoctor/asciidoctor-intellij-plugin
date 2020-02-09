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
import org.jetbrains.annotations.NotNull;

public class AsciiDocTextMono extends ASTWrapperPsiElement {
  public AsciiDocTextMono(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocTextMono> {

    /**
     * This method will be called when either the anchor or the file is renamed.
     * It will find the appropriate child element and rename it.
     * <p>
     * Limitation: If the content change would spread two children, an out-of-bounds-exception would occur.
     * Situation when this would happen is unclear, therefore not implemented for now.
     */
    @Override
    public AsciiDocTextMono handleContentChange(@NotNull AsciiDocTextMono element,
                                                @NotNull TextRange range,
                                                String newContent) throws IncorrectOperationException {
      PsiElement child = element.getFirstChild();
      if (child instanceof LeafPsiElement) {
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
    public TextRange getRangeInElement(@NotNull AsciiDocTextMono element) {
      return getBodyRange(element);
    }
  }

  public static TextRange getBodyRange(AsciiDocTextMono element) {
    return element.getTextRange();
  }


}

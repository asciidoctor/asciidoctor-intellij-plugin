package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocAddAdocExtensionToXref extends AsciiDocLocalQuickFix {

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.addAdocExtentionToXref");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element instanceof AsciiDocLink) {
      AsciiDocLink link = (AsciiDocLink) element;
      LeafPsiElement child = getChild(link);
      if (child != null) {
        AsciiDocFileReference fileReference = link.getFileReference();
        if (fileReference != null) {
          ResolveResult[] resolveResultsFile = fileReference.multiResolve(false);
          if (resolveResultsFile.length == 1) {
            child.replaceWithText(fileReference.getRangeInElement().shiftLeft(child.getStartOffsetInParent()).replace(child.getText(), child.getText() + ".adoc"));
          }
        }
      }
    }
  }

  public boolean canFix(AsciiDocLink link) {
    return getChild(link) != null;
  }

  @Nullable
  private LeafPsiElement getChild(AsciiDocLink link) {
    AsciiDocFileReference fileReference = link.getFileReference();
    if (fileReference != null) {
      ResolveResult[] resolveResultsFile = fileReference.multiResolve(false);
      if (resolveResultsFile.length == 1) {
        PsiElement child = link.getFirstChild();
        TextRange range = fileReference.getRangeInElement();
        while (child != null && range.getStartOffset() >= child.getTextLength()) {
          range = range.shiftRight(-child.getTextLength());
          child = child.getNextSibling();
        }
        if (child instanceof LeafPsiElement && range.getEndOffset() <= child.getTextLength()
          && child.getNode() != null && child.getNode().getElementType() == AsciiDocTokenTypes.LINKFILE) {
          return (LeafPsiElement) child;
        }
      }
    }
    return null;
  }


}

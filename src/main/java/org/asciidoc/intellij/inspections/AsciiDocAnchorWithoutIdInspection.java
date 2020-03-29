package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.HasAnchorReference;
import org.asciidoc.intellij.quickfix.AsciiDocAddBlockIdToSection;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocAnchorWithoutIdInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_ANCHOR_WITHOUT_ID = "Section doesn't have explicit block ID";
  private static final AsciiDocAddBlockIdToSection ADD_BLOCK_ID_TO_SECTION = new AsciiDocAddBlockIdToSection();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(PsiElement o) {
        if (o instanceof HasAnchorReference) {
          AsciiDocSection section = ((HasAnchorReference) o).resolveAnchorForSection();
          AsciiDocFileReference anchor = ((HasAnchorReference) o).getAnchorReference();
          if (section != null && anchor != null && section.getBlockId() == null && !anchor.isPossibleRefText()) {
            LocalQuickFix[] fixes = new LocalQuickFix[]{ADD_BLOCK_ID_TO_SECTION};
            holder.registerProblem(o, TEXT_HINT_ANCHOR_WITHOUT_ID, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, anchor.getRangeInElement(), fixes);
          }
        }
        super.visitElement(o);
      }
    };
  }
}

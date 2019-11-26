package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.quickfix.AsciiDocShortenPagebreak;
import org.jetbrains.annotations.NotNull;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.PAGEBREAK;

/**
 * @author Alexander Schwartz 2016
 */
public class AsciiDocPageBreakInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_PAGEBREAK = "Too long Pagebreak";
  private static final AsciiDocShortenPagebreak PAGEBREAK_SHORTEN_QUICKFIX = new AsciiDocShortenPagebreak();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(PsiElement o) {
        if (o != null && o.getNode().getElementType() == PAGEBREAK) {
          if (o.getNode().getText().trim().length() > 3) {
            LocalQuickFix[] fixes = new LocalQuickFix[]{PAGEBREAK_SHORTEN_QUICKFIX};
            holder.registerProblem(o, TEXT_HINT_PAGEBREAK, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
          }
        }
        super.visitElement(o);
      }
    };
  }
}

package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocTextQuoted;
import org.asciidoc.intellij.quickfix.AsciiDocConvertPassthrough;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz
 */
public class AsciiDocPassthroughInspection extends AsciiDocInspectionBase {
  public static final String OBSOLETE_PASSTRHOUGH = "$$";
  private static final String TEXT_HINT_PASSTHROUGH = "Obsolete Passthrough";

  private static final AsciiDocConvertPassthrough OBSOLETE_PASSTHROUGH_QUICKFIX = new AsciiDocConvertPassthrough();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder,
                                                 @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement e) {
        if (isQuotedTextWithObsoletePassthrough(e)) {
          final LocalQuickFix[] fixes = new LocalQuickFix[]{OBSOLETE_PASSTHROUGH_QUICKFIX};
          holder.registerProblem(e, TEXT_HINT_PASSTHROUGH, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
        } else if ((isObsoletePassthroughStart(e) || isObsoletePassthroughEnd(e))
        && !isQuotedTextWithObsoletePassthrough(e.getParent())) {
          final LocalQuickFix[] fixes = new LocalQuickFix[]{OBSOLETE_PASSTHROUGH_QUICKFIX};
          holder.registerProblem(e, TEXT_HINT_PASSTHROUGH, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
        } else {
          super.visitElement(e);
        }
      }
    };
  }

  public static boolean isQuotedTextWithObsoletePassthrough(@NotNull PsiElement e) {
    return e instanceof AsciiDocTextQuoted && isObsoletePassthroughStart(e.getFirstChild())
      && isObsoletePassthroughEnd(e.getLastChild());
  }

  public static boolean isObsoletePassthroughStart(PsiElement e) {
    return e.getNode() != null && e.getNode().getElementType() == AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START
      && e.getNode().getText().equals(OBSOLETE_PASSTRHOUGH);
  }

  public static boolean isObsoletePassthroughEnd(PsiElement e) {
    return e.getNode() != null && e.getNode().getElementType() == AsciiDocTokenTypes.PASSTRHOUGH_INLINE_END
      && e.getNode().getText().equals(OBSOLETE_PASSTRHOUGH);
  }

}

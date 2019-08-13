package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.quickfix.AsciiDocConvertAttributeContinuationLegacy;
import org.jetbrains.annotations.NotNull;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION_LEGACY;

public class AsciiDocAttributeContinuationInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_LEGACY = "Attribute legacy continuation";
  private static final AsciiDocConvertAttributeContinuationLegacy ATTRIBUTE_CONTINUATION_LEGACY_QUICKFIX
    = new AsciiDocConvertAttributeContinuationLegacy();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(PsiElement o) {
        if (o != null && o.getNode().getElementType() == ATTRIBUTE_CONTINUATION_LEGACY) {
          LocalQuickFix[] fixes = new LocalQuickFix[]{ATTRIBUTE_CONTINUATION_LEGACY_QUICKFIX};
          holder.registerProblem(o, TEXT_HINT_LEGACY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
        }
        super.visitElement(o);
      }
    };
  }
}

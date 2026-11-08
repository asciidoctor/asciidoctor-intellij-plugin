package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocRef;
import org.asciidoc.intellij.quickfix.AsciiDocChangeShorthandXrefToXrefMacro;
import org.jetbrains.annotations.NotNull;

/**
 * Warn about the shorthand cross-reference syntax {@code <<file.adoc#anchor,text>>} that points to another document.
 * The double angle brackets should only be used for references within the current document.
 */
public class AsciiDocInterDocumentShorthandXrefInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_SHORTHAND_XREF = "Use the xref macro for a cross-reference to another document; the shorthand syntax is only for references within the same document";
  private static final AsciiDocChangeShorthandXrefToXrefMacro CHANGE_TO_XREF = new AsciiDocChangeShorthandXrefToXrefMacro();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
        if (o instanceof AsciiDocRef ref && AsciiDocChangeShorthandXrefToXrefMacro.isInterDocument(ref)) {
          LocalQuickFix[] fixes = null;
          if (CHANGE_TO_XREF.canFix(ref)) {
            fixes = new LocalQuickFix[]{CHANGE_TO_XREF};
          }
          holder.registerProblem(o, TEXT_HINT_SHORTHAND_XREF, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            AsciiDocChangeShorthandXrefToXrefMacro.getTarget(ref).getTextRangeInParent(), fixes);
        }
        super.visitElement(o);
      }
    };
  }
}

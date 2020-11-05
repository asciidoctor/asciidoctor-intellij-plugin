package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.quickfix.AsciiDocChangeXrefWithNaturalCrossReferenceToId;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocXrefWithNaturalCrossReferenceInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_NO_NATURAL_CROSS_REFERENCE = "An xref macro should not contain a natural cross reference.";
  private static final AsciiDocChangeXrefWithNaturalCrossReferenceToId CHANGE_TO_INLINE_REF = new AsciiDocChangeXrefWithNaturalCrossReferenceToId();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
        if (o instanceof AsciiDocLink) {
          AsciiDocLink link = (AsciiDocLink) o;
          if (link.getMacroName().equals("xref")) {
            AsciiDocFileReference reference = link.getAnchorReference();
            if (reference != null) {
              if (reference.inspectAntoraXrefWithoutNaturalReference()) {
                LocalQuickFix[] fixes = null;
                if (CHANGE_TO_INLINE_REF.canFix(link)) {
                  // offer a quick fix only if it can be fixed
                  fixes = new LocalQuickFix[]{CHANGE_TO_INLINE_REF};
                }
                holder.registerProblem(o, TEXT_HINT_NO_NATURAL_CROSS_REFERENCE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, reference.getRangeInElement(), fixes);
              }
            }
          }
        }
        super.visitElement(o);
      }
    };
  }
}

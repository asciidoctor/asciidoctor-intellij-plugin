package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.quickfix.AsciiDocAddAdocExtensionToXref;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocXrefWithoutExtensionInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_ALWAYS_WITH_FILE_EXTENSION = "Antora xref should always have a file extension; skipping the extension '.adoc' is deprecated from Antora 3.0 onwards";
  private static final AsciiDocAddAdocExtensionToXref ADD_ADOC_EXTENSION = new AsciiDocAddAdocExtensionToXref();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
        if (o instanceof AsciiDocLink link) {
          
          if (link.getMacroName().equals("xref")) {
            AsciiDocFileReference reference = link.getFileReference();
            if (reference != null) {
              if (reference.inspectAntoraXrefWithoutExtension()) {
                LocalQuickFix[] fixes = null;
                if (ADD_ADOC_EXTENSION.canFix(link)) {
                  // offer a quick fix only if it can be fixed
                  fixes = new LocalQuickFix[]{ADD_ADOC_EXTENSION};
                }
                holder.registerProblem(o, TEXT_HINT_ALWAYS_WITH_FILE_EXTENSION, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, reference.getRangeInElement(), fixes);
              }
            }
          }
        }
        super.visitElement(o);
      }
    };
  }

}

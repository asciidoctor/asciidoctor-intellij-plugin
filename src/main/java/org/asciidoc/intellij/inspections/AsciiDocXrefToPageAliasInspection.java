package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.quickfix.AsciiDocChangeXrefFromPageAliasToPage;
import org.jetbrains.annotations.NotNull;

public class AsciiDocXrefToPageAliasInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_PAGE_ALIAS = "This xref points to a page alias; update it to the page's current location";
  private static final AsciiDocChangeXrefFromPageAliasToPage CHANGE_TO_PAGE = new AsciiDocChangeXrefFromPageAliasToPage();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
        if (o instanceof AsciiDocLink link && link.getMacroName().equals("xref")) {
          AsciiDocFileReference reference = link.getFileReference();
          if (reference != null && reference.inspectAntoraXrefViaPageAlias()) {
            LocalQuickFix[] fixes = null;
            if (CHANGE_TO_PAGE.canFix(link)) {
              fixes = new LocalQuickFix[]{CHANGE_TO_PAGE};
            }
            // highlight the complete resource ID, including component, version and module
            PsiElement linkFile = AsciiDocChangeXrefFromPageAliasToPage.getLinkFile(link);
            TextRange range = linkFile != null ? linkFile.getTextRangeInParent() : reference.getRangeInElement();
            holder.registerProblem(o, TEXT_HINT_PAGE_ALIAS, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, range, fixes);
          }
        }
        super.visitElement(o);
      }
    };
  }
}

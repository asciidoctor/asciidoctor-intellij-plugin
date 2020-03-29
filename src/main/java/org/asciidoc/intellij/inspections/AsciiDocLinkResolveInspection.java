package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.HasAnchorReference;
import org.asciidoc.intellij.psi.HasFileReference;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFile;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFileQuickfix;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocLinkResolveInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_FILE_DOESNT_RESOLVE = "File doesn't resolve";
  private static final String TEXT_HINT_ANCHOR_DOESNT_RESOLVE = "Anchor doesn't resolve";
  private static final AsciiDocChangeCaseForAnchor CHANGE_CASE_FOR_ANCHOR = new AsciiDocChangeCaseForAnchor();
  private static final AsciiDocCreateMissingFileQuickfix CREATE_FILE = new AsciiDocCreateMissingFileQuickfix();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(PsiElement o) {
        boolean tryToResolveAnchor = true;
        if (o instanceof HasFileReference) {
          AsciiDocFileReference file = ((HasFileReference) o).getFileReference();
          if (file != null) {
            ResolveResult[] resolveResults = file.multiResolve(false);
            if (resolveResults.length == 0) {
              LocalQuickFix[] fixes = new LocalQuickFix[]{};
              if (AsciiDocCreateMissingFile.isAvailable(o)) {
                fixes = new LocalQuickFix[]{CREATE_FILE};
              }
              holder.registerProblem(o, TEXT_HINT_FILE_DOESNT_RESOLVE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                file.getRangeInElement(), fixes);
              tryToResolveAnchor = false;
            } else if (resolveResults.length > 1) {
              tryToResolveAnchor = false;
            }
          }
        }
        if (tryToResolveAnchor && o instanceof HasAnchorReference) {
          AsciiDocFileReference anchor = ((HasAnchorReference) o).getAnchorReference();
          if (anchor != null) {
            ResolveResult[] resolveResultsAnchor = anchor.multiResolve(false);
            if (resolveResultsAnchor.length == 0) {
              ResolveResult[] resolveResultsAnchorCaseInsensitive = anchor.multiResolveAnchor(true);
              LocalQuickFix[] fixes = new LocalQuickFix[]{};
              if (resolveResultsAnchorCaseInsensitive.length == 1) {
                fixes = new LocalQuickFix[]{CHANGE_CASE_FOR_ANCHOR};
              }
              holder.registerProblem(o, TEXT_HINT_ANCHOR_DOESNT_RESOLVE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, anchor.getRangeInElement(), fixes);
            }
          }
        }
        super.visitElement(o);
      }
    };
  }
}

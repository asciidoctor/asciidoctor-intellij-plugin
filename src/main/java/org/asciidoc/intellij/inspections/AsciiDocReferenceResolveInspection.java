package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import org.asciidoc.intellij.psi.AsciiDocRef;
import org.asciidoc.intellij.psi.AsciiDocReference;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocReferenceResolveInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_ANCHOR_DOESNT_RESOLVE = "Anchor doesn't resolve";

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(PsiElement o) {
        if (o instanceof AsciiDocRef) {
          for (PsiReference reference : o.getReferences()) {
            if (reference instanceof AsciiDocReference) {
              AsciiDocReference asciiDocReference = (AsciiDocReference) reference;
              if (asciiDocReference.patternIsValid()) {
                ResolveResult[] resolveResultsAnchor = asciiDocReference.multiResolve(false);
                if (resolveResultsAnchor.length == 0) {
                  holder.registerProblem(o, TEXT_HINT_ANCHOR_DOESNT_RESOLVE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, asciiDocReference.getRangeInElement());
                }
              }
            }
          }
          super.visitElement(o);
        }
      }
    };
  }
}

package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.quickfix.AsciiDocChangeToInlineMacro;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocBlockMacroShouldBeInlineMacroInspection extends AsciiDocInspectionBase {
  private static final AsciiDocChangeToInlineMacro CHANGE_TO_INLINE_MACRO = new AsciiDocChangeToInlineMacro();
  public static final List<String> INLINE_MACROS = Arrays.asList("xref");

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
        if (o instanceof AsciiDocBlockMacro) {
          AsciiDocBlockMacro macro = (AsciiDocBlockMacro) o;
          if (INLINE_MACROS.contains(macro.getMacroName())) {
            LocalQuickFix[] fixes = new LocalQuickFix[]{CHANGE_TO_INLINE_MACRO};
            String message = AsciiDocBundle.message("asciidoc.blockMacroShouldBeInlineMacro.message", macro.getMacroName());
            holder.registerProblem(o, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, TextRange.from(0, macro.getMacroName().length() + 2), fixes);
          }
          super.visitElement(o);
        }
      }
    };
  }
}

package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.psi.AsciiDocInlineMacro;
import org.asciidoc.intellij.quickfix.AsciiDocChangeToBlockOrPreprocessorMacro;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocInlineMacroShouldBeBlockOrPreprocessorMacroInspection extends AsciiDocInspectionBase {
  public static final List<String> PREPROCESSOR_MACROS = Arrays.asList("include", "ifdef", "ifndef", "ifeval");
  public static final List<String> BLOCK_MACROS = Arrays.asList("toc");

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
        if (o instanceof AsciiDocInlineMacro) {
          AsciiDocInlineMacro macro = (AsciiDocInlineMacro) o;
          if (PREPROCESSOR_MACROS.contains(macro.getMacroName())) {
            LocalQuickFix[] fixes = new LocalQuickFix[]{new AsciiDocChangeToBlockOrPreprocessorMacro("preprocessor")};
            String message = AsciiDocBundle.message("asciidoc.inlineMacroShouldBeBlockOrPreprocessorMacro.message", macro.getMacroName(), "preprocessor");
            holder.registerProblem(o, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, TextRange.from(0, macro.getMacroName().length() + 1), fixes);
          }
          if (BLOCK_MACROS.contains(macro.getMacroName())) {
            LocalQuickFix[] fixes = new LocalQuickFix[]{new AsciiDocChangeToBlockOrPreprocessorMacro("block")};
            String message = AsciiDocBundle.message("asciidoc.inlineMacroShouldBeBlockOrPreprocessorMacro.message", macro.getMacroName(), "block");
            holder.registerProblem(o, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, TextRange.from(0, macro.getMacroName().length() + 1), fixes);
          }
          super.visitElement(o);
        }
      }
    };
  }
}

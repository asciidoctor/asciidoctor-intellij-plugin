package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.quickfix.AsciiDocConvertMarkdownHorizontalRule;
import org.jetbrains.annotations.NotNull;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HORIZONTALRULE;

public class AsciidocHorizontalRuleInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_MARKDOWN = "Markdown Horizontal Rule";
  private static final AsciiDocConvertMarkdownHorizontalRule MARKDOWN_HORIZONTAL_RULE_QUICKFIX = new AsciiDocConvertMarkdownHorizontalRule();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(PsiElement o) {
        if (o instanceof PsiElement && o.getNode().getElementType() == HORIZONTALRULE) {
          if (o.getNode().getText().startsWith("-") || o.getNode().getText().startsWith("*") || o.getNode().getText().startsWith("_")) {
            LocalQuickFix[] fixes = new LocalQuickFix[]{MARKDOWN_HORIZONTAL_RULE_QUICKFIX};
            holder.registerProblem(o, TEXT_HINT_MARKDOWN, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
          }
        }
        super.visitElement(o);
      }
    };
  }
}

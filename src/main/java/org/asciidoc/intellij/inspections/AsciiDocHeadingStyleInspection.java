package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocHeading;
import org.asciidoc.intellij.quickfix.AsciiDocConvertMarkdownHeading;
import org.asciidoc.intellij.quickfix.AsciiDocConvertOldstyleHeading;
import org.jetbrains.annotations.NotNull;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HEADING_OLDSTYLE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HEADING_TOKEN;

/**
 * @author Alexander Schwartz 2016
 */
public class AsciiDocHeadingStyleInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_MARKDOWN = "Markdown style heading";
  private static final AsciiDocConvertMarkdownHeading MARKDOWN_HEADING_QUICKFIX = new AsciiDocConvertMarkdownHeading();

  private static final String TEXT_HINT_OLD_STYLE = "AsciiDoc old style heading";
  private static final AsciiDocConvertOldstyleHeading OLDSTYLE_HEADING_QUICKFIX = new AsciiDocConvertOldstyleHeading();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(PsiElement o) {
        if (o instanceof AsciiDocHeading) {
          if (o.getNode().getFirstChildNode().getElementType() == HEADING_TOKEN) {
            if (o.getNode().getText().startsWith("#")) {
              LocalQuickFix[] fixes = new LocalQuickFix[]{MARKDOWN_HEADING_QUICKFIX};
              holder.registerProblem(o, TEXT_HINT_MARKDOWN, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
            }
          } else if (o.getNode().getFirstChildNode().getElementType() == HEADING_OLDSTYLE) {
            LocalQuickFix[] fixes = new LocalQuickFix[]{OLDSTYLE_HEADING_QUICKFIX};
            holder.registerProblem(o, TEXT_HINT_OLD_STYLE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
          }
        }
        super.visitElement(o);
      }
    };
  }
}

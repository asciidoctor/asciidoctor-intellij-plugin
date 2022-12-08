package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocInlineMacro;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocChangeToBlockOrPreprocessorMacro extends AsciiDocLocalQuickFix {
  private final String macroType;

  public AsciiDocChangeToBlockOrPreprocessorMacro(String macroType) {
    this.macroType = macroType;
  }

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.inlineMacroShouldBeBlockOrPreprocessorMacro.fix", macroType);
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element instanceof AsciiDocInlineMacro) {
      AsciiDocInlineMacro inlineMacro = (AsciiDocInlineMacro) element;
      ASTNode macroId = inlineMacro.getNode().getFirstChildNode();
      if (macroId.getElementType() == AsciiDocTokenTypes.INLINE_MACRO_ID) {
        String blockMacro = macroId.getText() + ":";
        String newText = blockMacro + TextRange.create(macroId.getTextLength(), element.getTextLength()).substring(element.getText());
        PsiElement newElement = createMacro(project, newText);
        inlineMacro.getParent().replace(newElement);
      }
    }
  }

  @NotNull
  private static PsiElement createMacro(@NotNull Project project, @NotNull String text) {
    AsciiDocFile file = AsciiDocUtil.createFileFromText(project, text + "\n");
    PsiElement child = file.getFirstChild();
    if (!child.getText().equals(text)) {
      throw new IllegalStateException("text differs: " + child.getText());
    }
    return child;
  }

}

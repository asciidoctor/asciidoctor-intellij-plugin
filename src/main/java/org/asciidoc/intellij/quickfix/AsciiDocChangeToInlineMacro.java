package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocChangeToInlineMacro extends AsciiDocLocalQuickFix {

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.changeToInlineMacro");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element instanceof AsciiDocBlockMacro) {
      AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) element;
      ASTNode macroId = blockMacro.getNode().getFirstChildNode();
      if (macroId.getElementType() == AsciiDocTokenTypes.BLOCK_MACRO_ID) {
        String inlineMacro = macroId.getText().replaceAll(":$", "");
        String newText = inlineMacro + TextRange.create(macroId.getTextLength(), element.getTextLength()).substring(element.getText());
        PsiElement newElement = createMacro(project, newText);
        blockMacro.replace(newElement);
      }
    }
  }

  @NotNull
  private static PsiElement createMacro(@NotNull Project project, @NotNull String text) {
    AsciiDocFile file = AsciiDocUtil.createFileFromText(project, text);
    PsiElement child = file.getFirstChild();
    if (child.getNextSibling() != null) {
      throw new IllegalStateException("created two children where there should only be one: " + child.getNextSibling().getText());
    }
    if (!child.getText().equals(text)) {
      throw new IllegalStateException("text differs: " + child.getText());
    }
    return child;
  }

}

package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.psi.AsciiDocAttributeReference;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocChangeToPassthrough extends AsciiDocLocalQuickFix {

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.changeToPassthrough");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element instanceof AsciiDocAttributeReference) {
      AsciiDocAttributeReference attributeReference = (AsciiDocAttributeReference) element;
      PsiElement newElement = createPassthrough(project, "+" + attributeReference.getText() + "+");
      attributeReference.replace(newElement);
    }
  }

  @NotNull
  private static PsiElement createPassthrough(@NotNull Project project, @NotNull String text) {
    AsciiDocFile file = AsciiDocUtil.createFileFromText(project, text);
    PsiElement child = file.getFirstChild();
    if (child.getNextSibling() != null) {
      throw new IllegalStateException("created two children where there should only be one: " + child.getNextSibling().getText());
    }
    child = child.getFirstChild();
    if (child.getNextSibling() != null) {
      throw new IllegalStateException("created two children where there should only be one: " + child.getNextSibling().getText());
    }
    if (!child.getText().equals(text)) {
      throw new IllegalStateException("text differs: " + child.getText());
    }
    return child;
  }

}

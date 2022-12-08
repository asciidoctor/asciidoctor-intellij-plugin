package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

public class AsciiDocConvertMarkdownHorizontalRule extends AsciiDocLocalQuickFix {

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.convertMarkdownHorizontalRule");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    element.replace(createHorizontalRule(project));
  }

  @NotNull
  private static PsiElement createHorizontalRule(@NotNull Project project) {
    AsciiDocFile file = AsciiDocUtil.createFileFromText(project, "'''\n");
    return file.getFirstChild();
  }

}

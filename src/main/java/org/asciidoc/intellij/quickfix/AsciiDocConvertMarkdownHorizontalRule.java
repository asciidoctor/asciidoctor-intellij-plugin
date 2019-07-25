package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFixBase;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.jetbrains.annotations.NotNull;

public class AsciiDocConvertMarkdownHorizontalRule extends LocalQuickFixBase {
  public static final String NAME = "Convert to AsciiDoc Horizontal Rule";

  public AsciiDocConvertMarkdownHorizontalRule() {
    super(NAME);
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    element.replace(createHorizontalRule(project, "'''\n"));
  }

  @NotNull
  public static PsiElement createHorizontalRule(@NotNull Project project, @NotNull String text) {
    AsciiDocFile file = createFileFromText(project, text);
    return file.getFirstChild();
  }

  @NotNull
  private static AsciiDocFile createFileFromText(@NotNull Project project, @NotNull String text) {
    return (AsciiDocFile) PsiFileFactory.getInstance(project).createFileFromText("a.adoc", AsciiDocLanguage.INSTANCE, text);
  }
}

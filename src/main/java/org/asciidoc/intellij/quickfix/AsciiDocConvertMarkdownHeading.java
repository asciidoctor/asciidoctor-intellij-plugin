package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFixBase;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2016
 */
public class AsciiDocConvertMarkdownHeading extends LocalQuickFixBase {
  public static final String NAME = "Convert to AsciiDoc Heading";

  public AsciiDocConvertMarkdownHeading() {
    super(NAME);
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    StringBuilder text = new StringBuilder(element.getText());
    for (int i = 0;i < text.length();++i) {
      if (text.charAt(i) == '#') {
        text.setCharAt(i, '=');
      } else {
        break;
      }
    }
    element.replace(createHeading(project, text.toString()));
  }

  @NotNull
  public static PsiElement createHeading(@NotNull Project project, @NotNull String text) {
    AsciiDocFile file = createFileFromText(project, text);
    return PsiTreeUtil.findChildOfType(file, AsciiDocSection.class).getFirstChild();
  }

  @NotNull
  private static AsciiDocFile createFileFromText(@NotNull Project project, @NotNull String text) {
    return (AsciiDocFile)PsiFileFactory.getInstance(project).createFileFromText("a.adoc", AsciiDocLanguage.INSTANCE, text);
  }
}

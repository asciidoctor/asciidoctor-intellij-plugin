package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFixBase;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2019
 */
public class AsciiDocShortenPagebreak extends LocalQuickFixBase {
  public static final String NAME = "Shorten Pagebreak";

  public AsciiDocShortenPagebreak() {
    super(NAME);
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    element.replace(createPagebreak(project, "<<<\n"));
  }

  @NotNull
  private static PsiElement createPagebreak(@NotNull Project project, @NotNull String text) {
    AsciiDocFile file = AsciiDocUtil.createFileFromText(project, text);
    return file.getFirstChild();
  }
}

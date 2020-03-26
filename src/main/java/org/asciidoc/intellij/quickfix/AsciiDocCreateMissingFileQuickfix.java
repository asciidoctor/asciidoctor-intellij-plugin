package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFixBase;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2019
 */
public class AsciiDocCreateMissingFileQuickfix extends LocalQuickFixBase implements AsciiDocCreateMissingFile {
  public static final String NAME = "Create the missing file";

  public AsciiDocCreateMissingFileQuickfix() {
    super(NAME);
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return super.getFamilyName();
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    applyFix(element, element.getProject());
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}

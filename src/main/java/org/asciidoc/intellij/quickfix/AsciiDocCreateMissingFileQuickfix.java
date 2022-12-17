package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2019
 */
public class AsciiDocCreateMissingFileQuickfix implements AsciiDocCreateMissingFile, LocalQuickFix {

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.createMissingFile");
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

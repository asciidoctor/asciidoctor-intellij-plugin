package org.asciidoc.intellij.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2019
 */
public class AsciiDocCreateMissingFileIntentionAction implements IntentionAction, AsciiDocCreateMissingFile {

  private final PsiElement element;

  public AsciiDocCreateMissingFileIntentionAction(PsiElement element) {
    this.element = element;
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getText() {
    return "Create the missing file";
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getFamilyName() {
    return "AsciiDoc";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return AsciiDocCreateMissingFile.isAvailable(element);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
    applyFix(element, project);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}

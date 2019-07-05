package org.asciidoc.intellij.actions.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.ui.InlineIncludeDialog;
import org.jetbrains.annotations.NotNull;

public class AsciiDocInlineIntention extends Intention {

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (AsciiDocFileType.INSTANCE != file.getFileType()) {
      return false;
    }
    PsiElement element = InlineIncludeDialog.getElement(editor, file);
    if (element == null) {
      return false;
    }
    PsiNamedElement resolved = InlineIncludeDialog.resolve(element);
    if (resolved == null) {
      return false;
    }
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PsiElement element = InlineIncludeDialog.getElement(editor, file);
    if (element == null) {
      return;
    }
    PsiNamedElement resolved = InlineIncludeDialog.resolve(element);
    if (resolved == null) {
      return;
    }
    new InlineIncludeDialog(project, element, resolved).show();
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}

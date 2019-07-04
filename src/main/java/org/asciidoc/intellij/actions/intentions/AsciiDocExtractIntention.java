package org.asciidoc.intellij.actions.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.ui.ExtractIncludeDialog;
import org.jetbrains.annotations.NotNull;

public class AsciiDocExtractIntention extends Intention {

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (AsciiDocFileType.INSTANCE != file.getFileType()) {
      return false;
    }
    if (editor.getSelectionModel().getSelectedText() != null) {
      return true;
    }
    PsiElement element = ExtractIncludeDialog.getElementToExtract(editor, file);
    if (element != null) {
      return true;
    }
    return false;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final ExtractIncludeDialog extractIncludeDialog = new ExtractIncludeDialog(project, editor, file);
    extractIncludeDialog.show();
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}

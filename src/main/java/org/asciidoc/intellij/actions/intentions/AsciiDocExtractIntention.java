package org.asciidoc.intellij.actions.intentions;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.ui.ExtractIncludeDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocExtractIntention extends Intention {

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (editor == null || file == null) {
      return false;
    }
    if (AsciiDocFileType.INSTANCE != file.getFileType()) {
      return false;
    }
    if (editor.getSelectionModel().getSelectedText() != null) {
      return true;
    }
    PsiDirectory dir = getPsiDirectory(project, file);
    if (dir == null) {
      // unable to determine current file's folder to create new include file later on
      return false;
    }
    PsiElement element = ExtractIncludeDialog.getElementToExtract(editor, file);
    if (element != null) {
      return true;
    }
    return false;
  }

  @Nullable
  public static PsiDirectory getPsiDirectory(Project project, PsiFile file) {
    PsiDirectory dir = file.getContainingDirectory();
    if (dir == null && project != null) {
      // special handling for language injection
      VirtualFile vf = file.getVirtualFile();
      if (vf.getParent() == null && vf instanceof VirtualFileWindow) {
        vf = ((VirtualFileWindow) vf).getDelegate();
      }
      vf = vf.getParent();
      if (vf != null) {
        dir = PsiManager.getInstance(project).findDirectory(vf);
      }
    }
    return dir;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PsiDirectory dir = getPsiDirectory(project, file);
    if (dir == null) {
      return;
    }
    final ExtractIncludeDialog extractIncludeDialog = new ExtractIncludeDialog(project, editor, file, dir);
    extractIncludeDialog.show();
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}

package org.asciidoc.intellij.actions.refactor;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.actions.asciidoc.AsciiDocAction;
import org.asciidoc.intellij.actions.intentions.AsciiDocExtractIntention;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.ui.ExtractIncludeDialog;
import org.jetbrains.annotations.NotNull;

public class ExtractIncludeAction extends AsciiDocAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {

    final PsiFile file = event.getData(LangDataKeys.PSI_FILE);
    final Project project = event.getProject();
    if (project == null || file == null) {
      return;
    }
    Editor editor = event.getData(LangDataKeys.EDITOR);
    if (editor == null) {
      return;
    }
    PsiDirectory dir = AsciiDocExtractIntention.getPsiDirectory(project, file);
    if (dir == null) {
      // unable to determine current file's folder to create new include file later on
      return;
    }
    final ExtractIncludeDialog extractIncludeDialog = new ExtractIncludeDialog(project, editor, file, dir);
    extractIncludeDialog.show();

  }

  @Override
  public void update(AnActionEvent event) {
    PsiFile file = event.getData(LangDataKeys.PSI_FILE);
    final Editor editor = event.getData(LangDataKeys.EDITOR);
    final Project project = event.getProject();
    boolean enabled = false;
    if (project != null && file != null && editor != null && file.getFileType() == AsciiDocFileType.INSTANCE) {
      PsiDirectory dir = AsciiDocExtractIntention.getPsiDirectory(project, file);
      if (dir != null) {
        enabled = true;
      }
    }
    event.getPresentation().setEnabledAndVisible(enabled);
  }

}

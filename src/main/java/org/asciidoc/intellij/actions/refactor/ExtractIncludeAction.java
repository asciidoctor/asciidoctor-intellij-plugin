package org.asciidoc.intellij.actions.refactor;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.actions.asciidoc.AsciiDocAction;
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

    final ExtractIncludeDialog extractIncludeDialog = new ExtractIncludeDialog(project, editor, file);
    extractIncludeDialog.show();

  }

}

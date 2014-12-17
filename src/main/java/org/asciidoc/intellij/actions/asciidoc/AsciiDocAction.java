package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.file.AsciiDocFileType;

/**
 * Actions extending this class only apply to AsciiDoc documents
 *
 * @author Erik Pragt
 */
public abstract class AsciiDocAction extends AnAction {

  @Override
  public void update(AnActionEvent event) {
    PsiFile file = event.getData(DataKeys.PSI_FILE);
    boolean enabled = false;

    if (file != null) {
      for (String ext : AsciiDocFileType.DEFAULT_ASSOCIATED_EXTENSIONS) {
        if (file.getName().endsWith("." + ext)) {
          enabled = true;
          break;
        }
      }
    }
    event.getPresentation().setEnabledAndVisible(enabled);
  }

}

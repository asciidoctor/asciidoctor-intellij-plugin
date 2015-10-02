package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Erik Pragt
 */
public abstract class FormatAsciiDocAction extends AsciiDocAction {


  public abstract String getName();

  public abstract String updateSelection(String selection);


  @Override
  public final void actionPerformed(@NotNull AnActionEvent event) {

    final Project project = event.getProject();
    if (project == null) {
      return;
    }
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }
    final Document document = editor.getDocument();

    SelectionModel selectionModel = editor.getSelectionModel();
    selectText(selectionModel);

    applyMarkup(selectionModel, document, project);
  }

  protected void selectText(SelectionModel selectionModel) {
    if (!selectionModel.hasSelection()) {
      selectionModel.selectWordAtCaret(false);
    }
  }

  private void applyMarkup(SelectionModel selectionModel, Document document, Project project) {
    int start = selectionModel.getSelectionStart();
    int end = selectionModel.getSelectionEnd();
    String text = selectionModel.getSelectedText();
    final String updatedText = updateSelection(text);
    updateDocument(project, document, start, end, updatedText);
  }

  private void updateDocument(final Project project, final Document document, final int start, final int end, final String updatedText) {
    final Runnable readRunner = new Runnable() {
      @Override
      public void run() {
        document.replaceString(start, end, updatedText);
      }
    };
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
          @Override
          public void run() {
            ApplicationManager.getApplication().runWriteAction(readRunner);
          }
        }, getName(), null);
      }
    });
  }

}

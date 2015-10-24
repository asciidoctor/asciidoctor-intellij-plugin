package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.asciidoc.intellij.ui.CreateTableDialog;
import org.jetbrains.annotations.NotNull;

public class CreateTableAction extends AsciiDocAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {

    final Project project = event.getProject();
    if (project == null) {
      return;
    }
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }

    final CreateTableDialog createTableDialog = new CreateTableDialog();
    createTableDialog.show();

    if (createTableDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
      final Document document = editor.getDocument();
      final int offset = editor.getCaretModel().getOffset();
      CommandProcessor.getInstance().executeCommand(project, new Runnable() {
        @Override
        public void run() {
          ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
              document.insertString(offset,
                  generateTable(
                      createTableDialog.getColumnCount(),
                      createTableDialog.getRowCount(),
                      createTableDialog.getTitle()));
            }
          });
        }
      }, null, null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
    }

 }


  private String generateTable(int cols, int rows, String title) {
    assert cols > 0;
    assert rows > 0;
    StringBuilder table = new StringBuilder("\n");
    if (!title.isEmpty()) {
      table.append(".").append(title).append("\n");
    }
    table.append("|===\n");
    // Create header columns
    for (int c = 0;c < cols;c++) {
      table.append("|Header ");
      table.append(c + 1);
      if (c < cols - 1) {
        table.append(" ");
      }
    }
    table.append("\n\n");
    // Create table cells
    for (int r = 0;r < rows;r++) {
      for (int c = 0;c < cols;c++) {
        // Build row
        table.append("|Column ");
        table.append(c + 1);
        table.append(", row ");
        table.append(r + 1);
        table.append("\n");
      }
      if (r < rows - 1) {
        table.append("\n");
      }
    }
    table.append("|===\n");
    return table.toString();
  }

}

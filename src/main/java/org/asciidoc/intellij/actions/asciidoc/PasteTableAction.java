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
import org.asciidoc.intellij.ui.PasteTableDialog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Action to import/convert table data from clipboard.
 */
public class PasteTableAction extends AsciiDocAction {

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

    ApplicationManager.getApplication().invokeLater(() -> {
      final PasteTableDialog pasteTableDialog = new PasteTableDialog();
      pasteTableDialog.show();

      if (pasteTableDialog.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
        final Document document = editor.getDocument();
        final int offset = editor.getCaretModel().getOffset();
        CommandProcessor.getInstance().executeCommand(project,
          () -> ApplicationManager.getApplication().runWriteAction(() -> {
            if (pasteTableDialog.getData() != null) {
              document.insertString(offset, toAsciiDocTable(pasteTableDialog.getData(), pasteTableDialog.getSeparator(), pasteTableDialog.isFirstLineHeader()));
            }
          }), null, null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION);
      }
    });
  }

  static String toAsciiDocTable(@NotNull String tableData, String separator, boolean firstLineHeader) {
    List<List<String>> rows = ClipboardTableParser.parse(tableData, separator);
    int cols = rows.stream().mapToInt(List::size).max().orElse(0);
    cols = Math.max(cols, 1);
    StringBuilder asciiDocTable = new StringBuilder("\n|===\n");

    if (!firstLineHeader) {
      // Create header columns
      for (int c = 0; c < cols; c++) {
        asciiDocTable.append("|Header ");
        asciiDocTable.append(c + 1);
        if (c < cols - 1) {
          asciiDocTable.append(' ');
        }
      }
      asciiDocTable.append("\n\n");
    }

    boolean firstLine = true;
    for (List<String> row : rows) {
      boolean header = firstLine && firstLineHeader;
      for (int i = 0; i < cols; i++) {
        asciiDocTable.append('|');
        if (i < row.size()) {
          asciiDocTable.append(escapeCell(row.get(i), header));
        }
        //new line except if line is the header (all headers must be on the same line)
        if (!header) {
          asciiDocTable.append('\n');
        }
      }
      if (header) {
        //header line must be followed by an empty line
        asciiDocTable.append('\n');
      }
      asciiDocTable.append('\n');
      firstLine = false;
    }
    asciiDocTable.append("|===\n");
    return asciiDocTable.toString();
  }

  private static String escapeCell(String cell, boolean header) {
    String result = cell.replace("\r\n", "\n").replace('\r', '\n').replace("|", "\\|");
    if (header) {
      // a header row must be on a single line
      result = result.replaceAll("\\s*\n\\s*", " ");
    }
    return result;
  }

}

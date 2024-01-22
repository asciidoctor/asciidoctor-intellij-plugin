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
import org.apache.commons.lang3.StringUtils;
import org.asciidoc.intellij.ui.PasteTableDialog;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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

  private String toAsciiDocTable(@NotNull String tableData, String separator, boolean firstLineHeader) {
    StringBuilder asciiDocTable = new StringBuilder("\n");
    int cols;
    try (BufferedReader br = new BufferedReader(new CharArrayReader(tableData.toCharArray()))) {
      cols = br.lines().mapToInt(line -> StringUtils.countMatches(line, separator)).max().orElse(0) + 1;

      asciiDocTable.append("|===\n");

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
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try (BufferedReader br = new BufferedReader(new CharArrayReader(tableData.toCharArray()))) {
      boolean firstLine = true;
      List<String> lines = br.lines().collect(Collectors.toList());
      for (String line : lines) {
        String[] elements = line.split(separator, -1);
        for (int i = 0; i < cols; i++) {
          asciiDocTable.append('|');
          if (i < elements.length) {
            asciiDocTable.append(elements[i]);
          }
          //new line except if line is the header (all headers must be on the same line)
          if (!firstLine || !firstLineHeader) {
            asciiDocTable.append('\n');
          }
        }
        if (firstLine && firstLineHeader) {
          //header line must be followed by an empty line
          asciiDocTable.append('\n');
        }
        asciiDocTable.append('\n');
        firstLine = false;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    asciiDocTable.append("|===\n");
    return asciiDocTable.toString();
  }

}

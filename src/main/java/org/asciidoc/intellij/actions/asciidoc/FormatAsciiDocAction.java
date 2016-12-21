package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.impl.TextRangeInterval;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Erik Pragt
 */
public abstract class FormatAsciiDocAction extends AsciiDocAction {


  public abstract String getName();

  public abstract String updateSelection(String selection, boolean word);

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

    selectText(editor);

    SelectionModel selectionModel = editor.getSelectionModel();
    boolean word = isWord(document, selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
    String selectedText = selectionModel.getSelectedText();
    if (selectedText == null) {
      selectedText = "";
    }
    String updatedText = updateSelection(selectedText, word);

    updateDocument(project, document, selectionModel, updatedText);
  }

  /**
   * Implementing the rules of Asciidoc's "When should I use unconstrained quotes?".
   * See http://asciidoctor.org/docs/user-manual/ for details.
   */
  protected static boolean isWord(Document document, int start, int end) {
    if (start > 0) {
      String preceededBy = document.getText(new TextRangeInterval(start - 1, start));
      // not a word if selection is preceeded by a semicolon, colon, an alphabetic characters, a digit or an underscore
      if (preceededBy.matches("(?U)[;:\\w_]")) {
        return false;
      }
    }
    if (start + 1 < document.getTextLength()) {
      String startingWith = document.getText(new TextRangeInterval(start, start + 1));
      // not a word if selection is starting with a whitespace
      if (startingWith.matches("[\\s]")) {
        return false;
      }
    }
    if (end < document.getTextLength()) {
      // not a word if followed by a alphabetic character, a digit or an underscore
      String succeededBy = document.getText(new TextRangeInterval(end, end + 1));
      if (succeededBy.matches("(?U)[\\w_]")) {
        return false;
      }
    }
    if (end > 0) {
      // not a word if selecting is ending with a whitespace
      String endsWith = document.getText(new TextRangeInterval(end - 1, end));
      if (endsWith.matches("[\\s]")) {
        return false;
      }
    }
    return true;
  }

  private static boolean isWhitespaceAtCaret(Editor editor) {
    final Document doc = editor.getDocument();

    final int offset = editor.getCaretModel().getOffset();
    if (offset >= doc.getTextLength()) return false;

    final char c = doc.getCharsSequence().charAt(offset);
    return c == ' ' || c == '\t' || c == '\n';
  }

  protected void selectText(Editor editor) {
    SelectionModel selectionModel = editor.getSelectionModel();
    if (!selectionModel.hasSelection()) {
      // if whitespace is at caret, the complete document would be selected, therefore check this first
      if (!isWhitespaceAtCaret(editor)) {
        selectionModel.selectWordAtCaret(false);
      }
    }
  }

  private void updateDocument(final Project project, final Document document, final SelectionModel selectionModel, final String updatedText) {
    final Runnable readRunner = new Runnable() {
      @Override
      public void run() {
        int start = selectionModel.getSelectionStart();
        int end = selectionModel.getSelectionEnd();
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

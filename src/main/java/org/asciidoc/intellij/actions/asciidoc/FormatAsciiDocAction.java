package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.impl.TextRangeInterval;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
 * @author Erik Pragt
 */
public abstract class FormatAsciiDocAction extends AsciiDocAction {


  public abstract String getName();

  public abstract String updateSelection(String selection, boolean word);

  public String getFormatCharacter() {
    return "";
  }

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
    final Document document = editor.getDocument();

    selectText(editor);

    SelectionModel selectionModel = editor.getSelectionModel();
    boolean word = isWord(document, selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), getFormatCharacter());
    String selectedText = selectionModel.getSelectedText();
    if (selectedText == null) {
      selectedText = "";
    }
    String updatedText = updateSelection(selectedText, word);

    updateDocument(project, document, selectionModel, editor.getCaretModel(), updatedText);
  }

  /**
   * Implementing the rules of Asciidoc's "When should I use unconstrained quotes?".
   * See http://asciidoctor.org/docs/user-manual/ for details.
   */
  public static boolean isWord(Document document, int start, int end, String formatChar) {
    if (start > 0) {
      String precededBy = document.getText(new TextRangeInterval(start - 1, start));
      // not a word if selection is preceded by a semicolon, colon, an alphabetic characters, a digit or an underscore
      if (precededBy.matches("(?U)[;:\\w_]")) {
        return false;
      }
    }
    if (start + 1 < document.getTextLength() && start != end) {
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
      if (succeededBy.matches("'") && formatChar.equals("`")) {
        // this would lead to a typographic quote, therefore use two quotes
        return false;
      }
    }
    if (end > 0 && start != end) {
      // not a word if selecting is ending with a whitespace
      String endsWith = document.getText(new TextRangeInterval(end - 1, end));
      if (endsWith.matches("[\\s]")) {
        return false;
      }
    }
    return true;
  }

  /** Check if character at cursor or left of cursor is a whitespace.
   */
  private static boolean isWhitespaceAtCaret(Editor editor) {
    final Document doc = editor.getDocument();

    final int offset = editor.getCaretModel().getOffset();
    if (offset >= doc.getTextLength()) {
      return true;
    }

    char c = doc.getCharsSequence().charAt(offset);
    if (isWhitespace(c)) {
      if (offset > 0) {
        // retrieve character left of cursor
        c = doc.getCharsSequence().charAt(offset - 1);
        return isWhitespace(c);
      }
      return true;
    }
    return false;
  }

  private static boolean isWhitespace(char c) {
    return c == ' ' || c == '\t' || c == '\n';
  }

  protected void selectText(Editor editor) {
    SelectionModel selectionModel = editor.getSelectionModel();
    if (!selectionModel.hasSelection()) {
      if (!isWhitespaceAtCaret(editor)) {
        // If the cursor is not at a whitespace, it is safe to expand the current selection like "Ctrl+W" in the IDE
        int selectionStart = selectionModel.getSelectionStart();
        int selectionEnd = selectionModel.getSelectionEnd();
        selectionModel.selectWordAtCaret(false);
        // if cursor was on empty line, whole document will be selected
        if (selectionModel.getSelectionStart() == 0 && selectionModel.getSelectionEnd() == editor.getDocument().getTextLength()) {
          selectionModel.setSelection(selectionStart, selectionEnd);
        }
      }
    }
  }

  private void updateDocument(final Project project, final Document document, final SelectionModel selectionModel, CaretModel caretModel, final String updatedText) {
    DocumentWriteAction.run(project, () -> {
      int start = selectionModel.getSelectionStart();
      int end = selectionModel.getSelectionEnd();
      String oldText = document.getText(TextRange.create(start, end));
      // try to change only the added/deleted bits so that the cursor position is updated accordingly
      if (updatedText.contains(oldText) && oldText.length() > 0) {
        int index = updatedText.indexOf(oldText);
        document.insertString(start, updatedText.substring(0, index));
        document.insertString(start + index + oldText.length(), updatedText.substring(index + oldText.length()));
        selectionModel.setSelection(start, start + updatedText.length());
      } else if (oldText.contains(updatedText)) {
        int index = oldText.indexOf(updatedText);
        document.deleteString(start + index + updatedText.length(), end);
        document.deleteString(start, start + index);
      } else {
        // if this is not possible, replace the contents, also for the special case that old string is empty
        document.replaceString(start, end, updatedText);
        if (start == end) {
          // if the old string was empty, place cursor in the middle of it
          caretModel.getCurrentCaret().moveToOffset(start + updatedText.length() / 2);
        } else {
          // update the selection to match the new text
          selectionModel.setSelection(start, start + updatedText.length());
        }
      }
    }, getName());
  }
}

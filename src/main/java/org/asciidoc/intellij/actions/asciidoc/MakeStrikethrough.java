package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;

/**
 * @author Erik Pragt
 */
public class MakeStrikethrough extends FormatAsciiDocAction {
  private static final String LINE_THROUGH = "[.line-through]";

  @Override
  public String getName() {
    return "MakeStrikethrough";
  }

  @Override
  protected void selectText(Editor editor) {
    super.selectText(editor);
    Caret currentCaret = editor.getCaretModel().getCurrentCaret();
    if (currentCaret.getSelectionStart() > LINE_THROUGH.length()
      && editor.getDocument().getText(TextRange.create(currentCaret.getSelectionStart() - LINE_THROUGH.length() - 1, currentCaret.getSelectionStart())).equals(LINE_THROUGH + "#")
      && currentCaret.getSelectionEnd() < editor.getDocument().getTextLength()
      && editor.getDocument().getText(TextRange.create(currentCaret.getSelectionEnd(), currentCaret.getSelectionEnd() + 1)).equals("#")) {
      currentCaret.setSelection(currentCaret.getSelectionStart() - LINE_THROUGH.length() - 1, currentCaret.getSelectionEnd() + 1);
    }
  }

  @Override
  public String updateSelection(String selection, boolean isWord) {
    String symbol = "#";

    if (selection.startsWith(LINE_THROUGH + symbol + symbol) && selection.endsWith(symbol + symbol)) {
      return selection.substring(LINE_THROUGH.length() + 2, selection.length() - 2);
    }

    if (selection.startsWith(LINE_THROUGH + symbol) && selection.endsWith(symbol)) {
      return selection.substring(LINE_THROUGH.length() + 1, selection.length() - 1);
    }

    if (!isWord) {
      symbol += symbol;
    }
    return LINE_THROUGH + symbol + selection + symbol;
  }
}

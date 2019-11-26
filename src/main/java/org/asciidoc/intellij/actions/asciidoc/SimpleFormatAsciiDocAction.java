package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;

/**
 * @author Michael Krausse (ehmkah)
 */
public abstract class SimpleFormatAsciiDocAction extends FormatAsciiDocAction {

  public abstract String getFormatCharacter();

  @Override
  protected void selectText(Editor editor) {
    super.selectText(editor);
    Caret currentCaret = editor.getCaretModel().getCurrentCaret();
    while (currentCaret.getSelectionStart() > 0
      && editor.getDocument().getText(TextRange.create(currentCaret.getSelectionStart() - 1, currentCaret.getSelectionStart())).equals(getFormatCharacter())
      && currentCaret.getSelectionEnd() < editor.getDocument().getTextLength()
      && editor.getDocument().getText(TextRange.create(currentCaret.getSelectionEnd(), currentCaret.getSelectionEnd() + 1)).equals(getFormatCharacter())) {
      currentCaret.setSelection(currentCaret.getSelectionStart() - 1, currentCaret.getSelectionEnd() + 1);
    }
  }

  @Override
  public String updateSelection(String selection, boolean isWord) {
    return updateSelectionIntern(selection, getFormatCharacter(), isWord);
  }

  protected String updateSelectionIntern(String selection, String symbol, boolean isWord) {
    if (containsSymbol(selection, symbol)) {
      return removeSymbol(selection, symbol);
    }
    return appendSymbol(selection, symbol, isWord);
  }

  private String appendSymbol(String selection, String symbol, boolean isWord) {
    String matchingSymbol = symbol;
    if (!isWord) {
      matchingSymbol += symbol;
    }
    return matchingSymbol + selection + matchingSymbol;
  }

  private String removeSymbol(String selection, String symbol) {
    if (selection.startsWith(symbol + symbol) && selection.endsWith(symbol + symbol)) {
      return removeSymbol(selection, 2);
    }
    return removeSymbol(selection, 1);
  }

  private String removeSymbol(String selection, int symbolLength) {
    if (selection.length() == symbolLength) {
      return "";
    }
    return selection.substring(symbolLength, selection.length() - symbolLength);
  }

  private boolean containsSymbol(String selection, String symbol) {
    String doubleSymbol = symbol + symbol;
    return (selection.startsWith(symbol) && selection.endsWith(symbol)) ||
      (selection.startsWith(doubleSymbol) && selection.endsWith(doubleSymbol));
  }
}

package org.asciidoc.intellij.actions.asciidoc;

/**
 * Created by Michael Krausse on 13/11/15.
 */
public abstract class SimpleFormatAsciiDocAction extends FormatAsciiDocAction {

  protected String updateSelectionIntern(String selection, String symbol) {
    if (containsSymbol(selection, symbol)) {
      return removeSymbol(selection, symbol);
    }
    return appendSymbol(selection, symbol);
  }

  private String appendSymbol(String selection, String symbol) {
    String doubleSymbol = symbol + symbol;
    return doubleSymbol + selection + doubleSymbol;
  }

  private String removeSymbol(String selection, String symbol) {
    if (selection.startsWith(symbol + symbol)) {
      return selection.substring(2, selection.length() - 2);
    }
    return selection.substring(1, selection.length() - 1);
  }

  private boolean containsSymbol(String selection, String symbol) {
    String doubleSymbol = symbol + symbol;
    return (selection.startsWith(symbol) && selection.endsWith(symbol)) ||
        (selection.startsWith(doubleSymbol) && selection.endsWith(doubleSymbol));
  }
}

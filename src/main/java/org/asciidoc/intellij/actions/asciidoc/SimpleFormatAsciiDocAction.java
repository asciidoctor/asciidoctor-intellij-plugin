package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Michael Krausse (ehmkah)
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
      return removeSymbolIntern(selection, symbol+symbol);
    }
    return removeSymbolIntern(selection, symbol);
  }

  private String removeSymbolIntern(String selection, String symbol) {
    if (selection.length() == symbol.length()) {
      return "";
    }
    return selection.substring(symbol.length(), selection.length() - symbol.length());
  }

  private boolean containsSymbol(String selection, String symbol) {
    String doubleSymbol = symbol + symbol;
    return (selection.startsWith(symbol) && selection.endsWith(symbol)) ||
        (selection.startsWith(doubleSymbol) && selection.endsWith(doubleSymbol));
  }
}

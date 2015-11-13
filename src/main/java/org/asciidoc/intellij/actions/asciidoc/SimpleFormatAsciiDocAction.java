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

package org.asciidoc.intellij.actions.asciidoc;

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

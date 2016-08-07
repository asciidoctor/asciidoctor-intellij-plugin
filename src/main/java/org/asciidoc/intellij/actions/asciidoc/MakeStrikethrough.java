package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeStrikethrough extends FormatAsciiDocAction {
  @Override
  public String getName() {
    return "MakeStrikethrough";
  }

  @Override
  public String updateSelection(String selection, boolean isWord) {
    if(selection.startsWith("[line-through]##") && selection.endsWith("##")) {
      return selection.substring(16, selection.length() - 2);
    }

    if(selection.startsWith("[line-through]#") && selection.endsWith("#")) {
      return selection.substring(15, selection.length() - 1);
    }

    String symbol = "#";
    if(!isWord) {
      symbol += symbol;
    }
    return "[line-through]" + symbol + selection + symbol;
  }
}

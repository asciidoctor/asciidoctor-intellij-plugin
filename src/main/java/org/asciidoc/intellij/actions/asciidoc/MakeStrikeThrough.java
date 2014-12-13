package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeStrikeThrough extends FormatAsciiDocAction {
  @Override
  public String getName() {
    return "MakeStrikeThrough";
  }

  @Override
  public String updateSelection(String asciidoc) {
    return "[line-through]#" + asciidoc + "#";
  }
}

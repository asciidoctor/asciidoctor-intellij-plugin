package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeTitle extends FormatAsciiDocAction {
  @Override
  public String getName() {
    return "MakeTitle";
  }

  @Override
  public String updateSelection(String asciidoc) {
    return "= "+asciidoc;
  }
}

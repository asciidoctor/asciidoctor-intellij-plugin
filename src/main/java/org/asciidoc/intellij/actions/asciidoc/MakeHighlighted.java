package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Alexander Schwartz
 */
public class MakeHighlighted extends SimpleFormatAsciiDocAction {

  @Override
  public String getFormatCharacter() {
    return "#";
  }

  @Override
  public String getName() {
    return "MakeHighlighted";
  }

}

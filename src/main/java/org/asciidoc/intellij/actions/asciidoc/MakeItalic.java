package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeItalic extends SimpleFormatAsciiDocAction {

  @Override
  public String getFormatCharacter() {
    return "_";
  }

  @Override
  public String getName() {
    return "MakeItalic";
  }

}

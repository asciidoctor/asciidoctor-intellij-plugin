package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeMono extends SimpleFormatAsciiDocAction {

  @Override
  public String getFormatCharacter() {
    return "`";
  }

  @Override
  public String getName() {
    return "MakeMono";
  }

}

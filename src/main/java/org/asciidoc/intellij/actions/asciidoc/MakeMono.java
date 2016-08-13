package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeMono extends SimpleFormatAsciiDocAction {

  @Override
  public String updateSelection(String selection, boolean isWord) {
    return updateSelectionIntern(selection, "`", isWord);
  }

  @Override
  public String getName() {
    return "MakeMono";
  }

}

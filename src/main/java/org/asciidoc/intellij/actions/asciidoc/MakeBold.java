package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeBold extends FormatAsciiDocAction {

  @Override
  public String updateSelection(String selection) {
    return updateSelectionIntern(selection, "*");
  }

  @Override
  public String getName() {
    return "MakeBold";
  }
}

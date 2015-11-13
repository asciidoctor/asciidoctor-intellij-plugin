package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeItalic extends SimpleFormatAsciiDocAction {

  @Override
  public String updateSelection(String selection) {
    return updateSelectionIntern(selection, "_");
  }

  @Override
  public String getName() {
    return "MakeItalic";
  }

}

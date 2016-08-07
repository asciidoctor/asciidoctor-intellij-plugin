package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Erik Pragt
 */
public class MakeItalic extends SimpleFormatAsciiDocAction {

  @Override
  public String updateSelection(String selection, boolean isWord) {
    return updateSelectionIntern(selection, "_", isWord);
  }

  @Override
  public String getName() {
    return "MakeItalic";
  }

}

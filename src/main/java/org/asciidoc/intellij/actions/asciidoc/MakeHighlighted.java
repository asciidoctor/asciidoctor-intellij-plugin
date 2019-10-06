package org.asciidoc.intellij.actions.asciidoc;

/**
 * @author Alexander Schwartz
 */
public class MakeHighlighted extends SimpleFormatAsciiDocAction {

  @Override
  public String updateSelection(String selection, boolean isWord) {
    return updateSelectionIntern(selection, "#", isWord);
  }

  @Override
  public String getName() {
    return "MakeHighlighted";
  }

}

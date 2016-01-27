package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.editor.SelectionModel;

/**
 * @author Michael Krausse, Raffael Krebs, Ulrich Etter
 */
public class MakeLink extends FormatAsciiDocAction {

  @Override
  public String getName() {
    return "MakeLink";
  }

  @Override
  public String updateSelection(String selection) {
    if (isLink(selection)) {
      return selection + "[]";
    }
    else {
      return "http://" + selection + "[" + selection + "]";
    }

  }

  boolean isLink(String selection) {
    return selection.startsWith("http://") || selection.startsWith("https://");
  }

  protected void selectText(SelectionModel selectionModel) {
    if (!selectionModel.hasSelection()) {
      selectionModel.selectWordAtCaret(false);
    }
  }


}

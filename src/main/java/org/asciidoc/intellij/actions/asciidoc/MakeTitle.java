package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.editor.SelectionModel;

/**
 * @author Erik Pragt
 */
public class MakeTitle extends FormatAsciiDocAction {
  @Override
  public String getName() {
    return "MakeTitle";
  }

  @Override
  public String updateSelection(String selection) {

    if (selection.startsWith("= ")) {
      return selection.substring(2);
    }
    return "= " + selection;
  }

  @Override
  protected void selectText(SelectionModel selectionModel) {
    selectionModel.selectLineAtCaret();
    selectionModel.setSelection(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
  }

}

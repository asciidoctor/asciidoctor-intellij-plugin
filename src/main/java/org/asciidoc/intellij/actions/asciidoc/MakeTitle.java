package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.editor.Editor;
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
  public boolean displayTextInToolbar() {
    // this doesn't have an icon, therefore show text
    return true;
  }

  @Override
  public String updateSelection(String selection, boolean isWord) {

    if (selection.startsWith("= ")) {
      return selection.substring(2);
    }
    return "= " + selection;
  }

  @Override
  protected void selectText(Editor editor) {
    SelectionModel selectionModel = editor.getSelectionModel();
    selectionModel.selectLineAtCaret();
    selectionModel.setSelection(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
  }

}

package org.asciidoc.intellij.actions.editorlayout;

import org.asciidoc.intellij.ui.SplitFileEditor;

public class EditorOnlyLayoutChangeAction extends BaseChangeSplitLayoutAction {
  protected EditorOnlyLayoutChangeAction() {
    super(SplitFileEditor.SplitEditorLayout.FIRST);
  }
}

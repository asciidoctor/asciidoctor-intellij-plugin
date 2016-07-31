package org.asciidoc.intellij.actions.editorlayout;

import org.asciidoc.intellij.ui.SplitFileEditor;

public class EditorAndPreviewLayoutChangeAction extends BaseChangeSplitLayoutAction {
  protected EditorAndPreviewLayoutChangeAction() {
    super(SplitFileEditor.SplitEditorLayout.SPLIT);
  }
}

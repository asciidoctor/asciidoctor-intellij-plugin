package org.asciidoc.intellij.actions.editorlayout;

import org.asciidoc.intellij.ui.SplitFileEditor;

public class PreviewOnlyLayoutChangeAction extends BaseChangeSplitLayoutAction {
  protected PreviewOnlyLayoutChangeAction() {
    super(SplitFileEditor.SplitEditorLayout.SECOND);
  }
}

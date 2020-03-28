package org.asciidoc.intellij.actions.editorlayout;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.DumbAware;
import org.asciidoc.intellij.actions.AsciiDocActionUtil;
import org.asciidoc.intellij.ui.SplitFileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class BaseChangeSplitLayoutAction extends AnAction implements DumbAware, Toggleable {
  @Nullable
  private final SplitFileEditor.SplitEditorLayout myLayoutToSet;

  protected BaseChangeSplitLayoutAction(@Nullable SplitFileEditor.SplitEditorLayout layoutToSet) {
    myLayoutToSet = layoutToSet;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final SplitFileEditor<?, ?> splitFileEditor = AsciiDocActionUtil.findSplitEditor(e);
    e.getPresentation().setEnabledAndVisible(splitFileEditor != null);

    if (myLayoutToSet != null && splitFileEditor != null) {
      Toggleable.setSelected(e.getPresentation(), splitFileEditor.getCurrentEditorLayout() == myLayoutToSet);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final SplitFileEditor<?, ?> splitFileEditor = AsciiDocActionUtil.findSplitEditor(e);

    if (splitFileEditor != null) {
      if (myLayoutToSet == null) {
        splitFileEditor.triggerLayoutChange(true);
      } else {
        splitFileEditor.triggerLayoutChange(myLayoutToSet, true);
        Toggleable.setSelected(e.getPresentation(), true);
      }
    }
  }
}

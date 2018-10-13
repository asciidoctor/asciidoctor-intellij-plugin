package org.asciidoc.intellij.actions.asciidoc;

import org.asciidoc.intellij.actions.AsciiDocActionUtil;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actions.AbstractToggleUseSoftWrapsAction;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;



public class ToggleSoftWrapsAction extends AbstractToggleUseSoftWrapsAction {
  public ToggleSoftWrapsAction() {
    super(SoftWrapAppliancePlaces.MAIN_EDITOR, false);
    copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_USE_SOFT_WRAPS));
  }


  @Nullable
  @Override
  protected Editor getEditor(AnActionEvent e) {
    final Editor editor = AsciiDocActionUtil.findAsciiDocTextEditor(e);return editor;
  }
}

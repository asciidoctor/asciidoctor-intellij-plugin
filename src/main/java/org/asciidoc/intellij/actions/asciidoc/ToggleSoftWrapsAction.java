package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actions.AbstractToggleUseSoftWrapsAction;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.actions.AsciiDocActionUtil;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToggleSoftWrapsAction extends AbstractToggleUseSoftWrapsAction implements DumbAware {

  public ToggleSoftWrapsAction() {
    super(SoftWrapAppliancePlaces.MAIN_EDITOR, false);
    copyFrom(ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_USE_SOFT_WRAPS));
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    PsiFile file = event.getData(LangDataKeys.PSI_FILE);
    boolean enabled = false;
    if (file != null) {
      for (String ext : AsciiDocFileType.DEFAULT_ASSOCIATED_EXTENSIONS) {
        if (file.getName().endsWith("." + ext)) {
          enabled = true;
          break;
        }
      }
    }
    Editor editor = getEditor(event);
    if (editor != null) {
      Toggleable.setSelected(event.getPresentation(), editor.getSettings().isUseSoftWraps());
    }
    event.getPresentation().setEnabledAndVisible(enabled);
  }

  @Nullable
  @Override
  protected Editor getEditor(@NotNull AnActionEvent e) {
    return AsciiDocActionUtil.findAsciiDocTextEditor(e);
  }
}

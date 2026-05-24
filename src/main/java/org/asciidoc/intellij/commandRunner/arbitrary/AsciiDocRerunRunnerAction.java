package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

class AsciiDocRerunRunnerAction extends AnAction {
  private AsciiDocBackgroundCommand asciiDocBackgroundCommand;

  AsciiDocRerunRunnerAction() {
    super("Rerun", "Rerun the command", AllIcons.Actions.Rerun);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    event.getPresentation().setEnabled(asciiDocBackgroundCommand != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    if (asciiDocBackgroundCommand != null) {
      asciiDocBackgroundCommand.rerun();
    }
  }

  public void setAsciiDocBackgroundCommand(AsciiDocBackgroundCommand asciiDocBackgroundCommand) {
    this.asciiDocBackgroundCommand = asciiDocBackgroundCommand;
  }
}

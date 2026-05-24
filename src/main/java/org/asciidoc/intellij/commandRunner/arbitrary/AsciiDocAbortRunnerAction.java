package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

class AsciiDocAbortRunnerAction extends AnAction {
  private AsciiDocBackgroundCommand asciiDocBackgroundCommand;

  AsciiDocAbortRunnerAction() {
    super("Abort", "Abort the running command", AllIcons.Actions.Suspend);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    event.getPresentation().setEnabled(asciiDocBackgroundCommand != null && asciiDocBackgroundCommand.isRunning());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    if (asciiDocBackgroundCommand != null) {
      asciiDocBackgroundCommand.abort();
    }
  }

  public void setAsciiDocBackgroundCommand(AsciiDocBackgroundCommand asciiDocBackgroundCommand) {
    this.asciiDocBackgroundCommand = asciiDocBackgroundCommand;
  }
}

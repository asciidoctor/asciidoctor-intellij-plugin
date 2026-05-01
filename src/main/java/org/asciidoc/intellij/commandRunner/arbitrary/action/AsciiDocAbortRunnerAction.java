package org.asciidoc.intellij.commandRunner.arbitrary.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.Setter;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocBackgroundCommand;
import org.jetbrains.annotations.NotNull;

@Setter
public class AsciiDocAbortRunnerAction extends AnAction {
  private AsciiDocBackgroundCommand asciiDocBackgroundCommand;

  public AsciiDocAbortRunnerAction() {
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
}

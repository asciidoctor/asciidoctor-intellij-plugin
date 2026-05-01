package org.asciidoc.intellij.commandRunner.arbitrary.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.Setter;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocBackgroundCommand;
import org.jetbrains.annotations.NotNull;

@Setter
public class AsciiDocRerunRunnerAction extends AnAction {
  private AsciiDocBackgroundCommand asciiDocBackgroundCommand;

  public AsciiDocRerunRunnerAction() {
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
}

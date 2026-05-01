package org.asciidoc.intellij.commandRunner.arbitrary.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.util.ui.TextTransferable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

public class AsciiDocCopyRunnerAction extends AnAction {
  @NotNull
  private final File tempFile;

  public AsciiDocCopyRunnerAction(@NotNull File tempFile) {
    super("Copy Temp-File Path", "Copy the path of the temporary file into the clipboard",
      AllIcons.Actions.Copy);
    Objects.requireNonNull(tempFile, "tempFile must not be null");
    this.tempFile = tempFile;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    // Copy the path of the temporary file to the clipboard
    CopyPasteManager.getInstance().setContents(new TextTransferable(tempFile.getAbsolutePath()));
  }
}

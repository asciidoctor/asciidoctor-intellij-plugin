package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public abstract class AsciiDocFileAction extends AsciiDocAction {
  @Override
  public void update(@NotNull AnActionEvent event) {
    VirtualFile file = event.getData(LangDataKeys.VIRTUAL_FILE);
    if (file == null) {
      event.getPresentation().setEnabledAndVisible(false);
      return;
    }
    try {
      file.toNioPath();
    } catch (UnsupportedOperationException ex) {
      // might happen if this is a JAR path
      event.getPresentation().setEnabledAndVisible(false);
      return;
    }

    super.update(event);
  }
}

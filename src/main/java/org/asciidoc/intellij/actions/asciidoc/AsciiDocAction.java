package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.ide.lightEdit.LightEditCompatible;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.serviceContainer.AlreadyDisposedException;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for creating AsciiDoc actions.
 * These actions will only apply to AsciiDoc documents.
 *
 * @author Erik Pragt
 */
public abstract class AsciiDocAction extends AnAction implements DumbAware, LightEditCompatible {

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    if (project != null && project.isDisposed()) {
      // otherwise classes like SingleRootFileViewProvider.createFile() might log an exception
      return;
    }
    boolean enabled = false;
    try {
      PsiFile file = event.getData(LangDataKeys.PSI_FILE);
      if (file != null) {
        if (file.getLanguage() == AsciiDocLanguage.INSTANCE) {
          enabled = true;
        } else {
          for (String ext : AsciiDocFileType.DEFAULT_ASSOCIATED_EXTENSIONS) {
            if (file.getName().endsWith("." + ext)) {
              enabled = true;
              break;
            }
          }
        }
      }
    } catch (AlreadyDisposedException ex) {
      // can happen if the module where this file belongs has been disposed.
      // ignored
    }
    event.getPresentation().setEnabledAndVisible(enabled);
  }

}

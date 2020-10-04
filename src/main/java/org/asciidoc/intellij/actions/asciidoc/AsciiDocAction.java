package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for creating AsciiDoc actions.
 * These actions will only apply to AsciiDoc documents.
 *
 * @author Erik Pragt
 */
public abstract class AsciiDocAction extends AnAction {

  @Override
  public void update(@NotNull AnActionEvent event) {
    PsiFile file = event.getData(LangDataKeys.PSI_FILE);
    boolean enabled = false;
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
    event.getPresentation().setEnabledAndVisible(enabled);
  }

}

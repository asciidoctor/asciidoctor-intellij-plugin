package org.asciidoc.intellij.actions.refactor;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.actions.asciidoc.AsciiDocAction;
import org.asciidoc.intellij.actions.intentions.AsciiDocAdmonitionToBlockIntention;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

/**
 * Wrap the AsciiDocAdmonitionToBlockIntention as an action, so that it can be called from the refactoring menu.
 *
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocAdmonitionToBlockAction extends AsciiDocAction {

  private final AsciiDocAdmonitionToBlockIntention intention = new AsciiDocAdmonitionToBlockIntention();

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    final PsiFile file = event.getData(LangDataKeys.PSI_FILE);
    final Project project = event.getProject();
    if (project == null || file == null) {
      return;
    }
    Editor editor = event.getData(LangDataKeys.EDITOR);
    if (editor == null) {
      return;
    }
    ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance().executeCommand(project,
      () -> intention.invoke(project, editor, file), null, null, UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION));
  }

  @Override
  public void update(AnActionEvent event) {
    PsiFile file = event.getData(LangDataKeys.PSI_FILE);
    final Editor editor = event.getData(LangDataKeys.EDITOR);
    final Project project = event.getProject();
    boolean enabled = false;
    if (project != null && file != null && editor != null && file.getFileType() == AsciiDocFileType.INSTANCE) {
      enabled = intention.isAvailable(project, editor, file);
    }
    event.getPresentation().setEnabledAndVisible(enabled);
  }

}

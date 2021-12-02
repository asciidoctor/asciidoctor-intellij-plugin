package org.asciidoc.intellij.injection;

import com.intellij.codeInsight.highlighting.HighlightErrorFilter;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocElementWithLanguage;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AsciiDocCodeFenceErrorHighlightingIntention implements IntentionAction {
  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getText() {
    return AsciiDocBundle.message("asciidoc.hide.errors.intention.text");
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  /**
   * {@inheritDoc}
   * This action is only available if there is an active code injection at the active element.
   */
  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (file.getFileType() != AsciiDocFileType.INSTANCE || AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isHideErrorsInSourceBlocks()) {
      return false;
    }
    PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
    if (element == null) {
      return false;
    }
    AsciiDocElementWithLanguage elementWithLanguage = PsiTreeUtil.getParentOfType(element, AsciiDocElementWithLanguage.class);
    if (elementWithLanguage == null) {
      return false;
    }
    List<Pair<PsiElement, TextRange>> injectedPsiFiles = InjectedLanguageManager.getInstance(project).getInjectedPsiFiles(elementWithLanguage);
    return injectedPsiFiles != null && injectedPsiFiles.size() > 0;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    setHideErrors(true);

    Notification notification = new Notification("asciidoctor", AsciiDocBundle.message("asciidoc.hide.errors.notification.title"),
      AsciiDocBundle.message("asciidoc.hide.errors.notification.content"), NotificationType.INFORMATION);
    notification.addAction(new NotificationAction(AsciiDocBundle.message("asciidoc.hide.errors.notification.rollback.action.text")) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
        setHideErrors(false);
        notification.expire();
      }
    });

    notification.notify(project);
  }

  private void setHideErrors(boolean hideErrors) {
    AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().setHideErrorsInSourceBlocks(hideErrors);

    ApplicationManager.getApplication().getMessageBus()
      .syncPublisher(AsciiDocApplicationSettings.SettingsChangedListener.TOPIC)
      .onSettingsChange(AsciiDocApplicationSettings.getInstance());
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  public static class CodeFenceHighlightErrorFilter extends HighlightErrorFilter {

    public static final String SOURCE_PREFIX = "source-";

    @Override
    public boolean shouldHighlightErrorElement(@NotNull PsiErrorElement element) {
      InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(element.getProject());
      PsiLanguageInjectionHost injectionHost = injectedLanguageManager.getInjectionHost(element);
      if (injectedLanguageManager.getTopLevelFile(element).getFileType() == AsciiDocFileType.INSTANCE
        && injectionHost instanceof AsciiDocElementWithLanguage) {
        if (AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isHideErrorsInSourceBlocks()) {
          return false;
        }
        String language = ((AsciiDocElementWithLanguage) injectionHost).getFenceLanguage();
        if (language != null && language.startsWith(SOURCE_PREFIX)) {
          if (AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getHiddenErrorsByLanguageAsList().contains(language.substring(SOURCE_PREFIX.length()))) {
            return false;
          }
        }
        return true;
      }
      return true;
    }
  }

}

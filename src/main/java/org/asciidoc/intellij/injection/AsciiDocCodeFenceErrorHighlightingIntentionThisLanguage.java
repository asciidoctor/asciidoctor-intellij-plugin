package org.asciidoc.intellij.injection;

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
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocElementWithLanguage;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AsciiDocCodeFenceErrorHighlightingIntentionThisLanguage implements IntentionAction {
  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getText() {
    return AsciiDocBundle.message("asciidoc.hide.errors.thisLanguage.intention.text");
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
    if (!elementWithLanguage.validateContent()) {
      return false;
    }
    String language = elementWithLanguage.getFenceLanguage();
    if (language != null) {
      if (AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getDisabledInjectionsByLanguageAsList().contains(language.substring(CodeFenceHighlightInfoFilter.SOURCE_PREFIX.length()))
        || AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getHiddenErrorsByLanguageAsList().contains(language.substring(CodeFenceHighlightInfoFilter.SOURCE_PREFIX.length()))) {
        return false;
      }
    }
    List<Pair<PsiElement, TextRange>> injectedPsiFiles = InjectedLanguageManager.getInstance(project).getInjectedPsiFiles(elementWithLanguage);
    return injectedPsiFiles != null && injectedPsiFiles.size() > 0;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
    if (element == null) {
      return;
    }
    AsciiDocElementWithLanguage elementWithLanguage = PsiTreeUtil.getParentOfType(element, AsciiDocElementWithLanguage.class);
    if (elementWithLanguage == null) {
      return;
    }
    if (!elementWithLanguage.validateContent()) {
      return;
    }
    String language = elementWithLanguage.getFenceLanguage().substring(CodeFenceHighlightInfoFilter.SOURCE_PREFIX.length());
    setHideErrors(true, language);

    Notification notification = new Notification("asciidoctor", AsciiDocBundle.message("asciidoc.hide.errors.notification.title"),
      AsciiDocBundle.message("asciidoc.hide.errors.notification.content"), NotificationType.INFORMATION);
    notification.addAction(new NotificationAction(AsciiDocBundle.message("asciidoc.hide.errors.notification.rollback.action.text")) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
        setHideErrors(false, language);
        notification.expire();
      }
    });

    notification.notify(project);
  }

  private void setHideErrors(boolean hideErrors, String language) {
    List<String> languages = AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getHiddenErrorsByLanguageAsList();
    if (hideErrors && !languages.contains(language)) {
      languages.add(language);
    } else {
      languages.remove(language);
    }

    AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().setHideErrorsByLanguage(String.join(";", languages));

    ApplicationManager.getApplication().getMessageBus()
      .syncPublisher(AsciiDocApplicationSettings.SettingsChangedListener.TOPIC)
      .onSettingsChange(AsciiDocApplicationSettings.getInstance());
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

}

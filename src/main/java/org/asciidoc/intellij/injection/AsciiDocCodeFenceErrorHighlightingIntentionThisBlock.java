package org.asciidoc.intellij.injection;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.injection.InjectedLanguageManager;
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
import org.asciidoc.intellij.psi.AsciiDocAttributeInBrackets;
import org.asciidoc.intellij.psi.AsciiDocBlockAttributes;
import org.asciidoc.intellij.psi.AsciiDocElementWithLanguage;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AsciiDocCodeFenceErrorHighlightingIntentionThisBlock implements IntentionAction {
  @Nls(capitalization = Nls.Capitalization.Sentence)
  @NotNull
  @Override
  public String getText() {
    return AsciiDocBundle.message("asciidoc.hide.errors.thisBlock.intention.text");
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

    AsciiDocBlockAttributes blockAttributes = PsiTreeUtil.findChildOfType(elementWithLanguage, AsciiDocBlockAttributes.class);

    if (blockAttributes == null) {
      return;

    }
    PsiElement child = blockAttributes.getFirstChild();
    boolean found = false;
    while (child != null) {
      if (child instanceof AsciiDocAttributeInBrackets a) {
        if (a.getAttrName().equals("opts")) {
          String value = a.getAttrValueUnresolved();
          if (value == null || value.isEmpty()) {
            value = "novalidate";
          } else {
            value = value + ",novalidate";
          }
          PsiElement newBlock = createAttributeBlock(project, value);
          found = true;
          child.replace(newBlock);
          break;
        }
      }
      child = child.getNextSibling();
    }

    if (!found) {
      PsiElement newBlock = createAttributeBlock(project, "novalidate");
      blockAttributes.addBefore(newBlock.getPrevSibling(), blockAttributes.getLastChild());
      blockAttributes.addBefore(newBlock, blockAttributes.getLastChild());
    }
  }

  private AsciiDocAttributeInBrackets createAttributeBlock(@NotNull Project project, String text) {
    if (text.contains(",")) {
      text = '"' + text + '"';
    }
    AsciiDocFile fileFromText = AsciiDocUtil.createFileFromText(project, "[,opts=" + text + "]\n----\n----\n");
    return PsiTreeUtil.findChildOfType(fileFromText, AsciiDocAttributeInBrackets.class);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

}

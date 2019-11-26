package org.asciidoc.intellij.injection;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.DeferredIconImpl;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LanguageListCompletionContributor extends CompletionContributor {

  @Override
  public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
    return typeChar == ',' && position.getNode().getElementType() == AsciiDocTokenTypes.ATTR_NAME
      && "source".equalsIgnoreCase(position.getNode().getText());
  }

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    final PsiElement completionElement = parameters.getPosition();
    if (PsiUtilCore.getElementType(completionElement) == AsciiDocTokenTypes.ATTR_NAME &&
      completionElement.getPrevSibling() != null &&
      completionElement.getPrevSibling().getPrevSibling() != null &&
      completionElement.getPrevSibling().getPrevSibling().getText().equals("source")) {
      doFillVariants(parameters, result);
    }
  }

  private static void doFillVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    for (Map.Entry<String, Language> entry : LanguageGuesser.INSTANCE.getLangToLanguageMap().entrySet()) {
      final Language language = entry.getValue();

      final LookupElementBuilder lookupElementBuilder =
        LookupElementBuilder.create(entry.getKey()).withIcon(new DeferredIconImpl<>(null, language, true, language1 -> {
          final LanguageFileType fileType = language1.getAssociatedFileType();
          return fileType != null ? fileType.getIcon() : null;
        }))
          .withTypeText(language.getDisplayName(), true);

      result.addElement(lookupElementBuilder);
    }
  }

}

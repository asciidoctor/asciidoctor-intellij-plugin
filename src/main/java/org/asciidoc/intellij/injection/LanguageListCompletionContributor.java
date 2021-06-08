package org.asciidoc.intellij.injection;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.DeferredIconImpl;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocAttributeInBrackets;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public class LanguageListCompletionContributor extends CompletionContributor {

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    final PsiElement completionElement = parameters.getPosition();
    if (PsiUtilCore.getElementType(completionElement) == AsciiDocTokenTypes.ATTR_NAME) {
      PsiElement element = completionElement.getParent();
      if (!(element instanceof AsciiDocAttributeInBrackets)) {
        return;
      }
      element = element.getPrevSibling();
      while (true) {
        if (element == null) {
          return;
        }
        if (element instanceof PsiWhiteSpace) {
          element = element.getPrevSibling();
          continue;
        }
        if (element.getNode().getElementType() == AsciiDocTokenTypes.SEPARATOR) {
          element = element.getPrevSibling();
          continue;
        }
        if (!(element instanceof AsciiDocAttributeInBrackets)) {
          return;
        }
        break;
      }
      if (Objects.equals(((AsciiDocAttributeInBrackets) element).getAttrName(), "source")) {
        doFillVariants(result);
      }
    }
  }

  private static void doFillVariants(@NotNull CompletionResultSet result) {
    for (Map.Entry<String, Language> entry : LanguageGuesser.getLangToLanguageMap().entrySet()) {
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

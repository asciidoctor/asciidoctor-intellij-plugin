package org.asciidoc.intellij.injection;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.psi.AsciiDocCodeContent;
import org.asciidoc.intellij.psi.AsciiDocListing;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class CodeFenceInjector implements MultiHostInjector {
  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    if (!(context instanceof AsciiDocListing)) {
      return;
    }
    if (PsiTreeUtil.findChildOfType(context, AsciiDocCodeContent.class) == null) {
      return;
    }

    final Language language = findLangForInjection(((AsciiDocListing) context));
    if (language == null || LanguageParserDefinitions.INSTANCE.forLanguage(language) == null) {
      return;
    }

    registrar.startInjecting(language);
    registrar.addPlace(null, null, ((AsciiDocListing) context), AsciiDocListing.getContentTextRange(context));
    registrar.doneInjecting();
  }

  @NotNull
  @Override
  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(AsciiDocListing.class);
  }

  @Nullable
  protected Language findLangForInjection(@NotNull AsciiDocListing element) {
    final String fenceLanguage = element.getFenceLanguage();
    if (fenceLanguage == null) {
      return null;
    }
    return guessLanguageByFenceLang(fenceLanguage);
  }

  @Nullable
  private static Language guessLanguageByFenceLang(@NotNull String langName) {
    if (!AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isEnabledInjections()) {
      return null;
    } else {
      return LanguageGuesser.INSTANCE.guessLanguage(langName);
    }
  }
}

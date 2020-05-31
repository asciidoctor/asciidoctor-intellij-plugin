package org.asciidoc.intellij.injection;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocElementWithLanguage;
import org.asciidoc.intellij.psi.AsciiDocFrontmatter;
import org.asciidoc.intellij.psi.AsciiDocListing;
import org.asciidoc.intellij.psi.AsciiDocPassthrough;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CodeFenceInjector implements MultiHostInjector {
  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    if (!(context instanceof AsciiDocElementWithLanguage)) {
      return;
    }

    final Language language = findLangForInjection((AsciiDocElementWithLanguage) context);
    if (language == null || LanguageParserDefinitions.INSTANCE.forLanguage(language) == null) {
      return;
    }

    registrar.startInjecting(language);
    registrar.addPlace(null, null, ((AsciiDocElementWithLanguage) context), ((AsciiDocElementWithLanguage) context).getContentTextRange());
    registrar.doneInjecting();
  }

  @NotNull
  @Override
  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Arrays.asList(AsciiDocListing.class, AsciiDocPassthrough.class, AsciiDocFrontmatter.class);
  }

  @Nullable
  protected Language findLangForInjection(@NotNull AsciiDocElementWithLanguage element) {
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
      return LanguageGuesser.guessLanguage(langName);
    }
  }
}

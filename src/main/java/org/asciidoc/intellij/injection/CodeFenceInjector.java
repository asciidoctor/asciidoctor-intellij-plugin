package org.asciidoc.intellij.injection;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
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
    if (!(context instanceof AsciiDocElementWithLanguage asciiDocElementWithLanguage)) {
      return;
    }

    final Language language = findLangForInjection(asciiDocElementWithLanguage);
    if (language == null || LanguageParserDefinitions.INSTANCE.forLanguage(language) == null) {
      return;
    }

    if (asciiDocElementWithLanguage.isValidHost()) {
      TextRange range = asciiDocElementWithLanguage.getContentTextRange();
      if (!range.equals(TextRange.EMPTY_RANGE)) {
        registrar.startInjecting(language);
        String prefix = null;
        String suffix = null;
        if (language.getID().equalsIgnoreCase("puml") && !range.substring(context.getText()).startsWith("@start")) {
          String fenceLanguage = asciiDocElementWithLanguage.getFenceLanguage();
          if (fenceLanguage.equals("diagram-plantuml")) {
            prefix = "@startuml\n";
            suffix = "\n@enduml";
          } else if (fenceLanguage.equals("diagram-salt")) {
            prefix = "@startsalt\n";
            suffix = "\n@endsalt";
          }
        }
        registrar.addPlace(prefix, suffix, asciiDocElementWithLanguage, range);
        registrar.doneInjecting();
      }
    }
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

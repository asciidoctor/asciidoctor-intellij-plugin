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
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.textmate.TextMateLanguage;
import org.jetbrains.plugins.textmate.TextMateService;
import org.jetbrains.plugins.textmate.bundles.TextMateFileNameMatcher;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Highlights code blocks with TextMate bundles when the IDE has no language for them, for example Go in IntelliJ IDEA Community.
 * This is a fallback to the {@link CodeFenceInjector}, and it is only registered when the TextMate plugin is available.
 * The approach is the same as in the Markdown plugin: inject the TextMate language and tell it the file extension
 * of the language, as TextMate selects the grammar by the file extension.
 */
public class TextMateCodeFenceInjector implements MultiHostInjector {
  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    if (!(context instanceof AsciiDocElementWithLanguage host) || !host.isValidHost()) {
      return;
    }
    AsciiDocPreviewSettings settings = AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings();
    if (!settings.isEnabledInjections()) {
      return;
    }
    String fenceLanguage = host.getFenceLanguage();
    if (fenceLanguage == null) {
      return;
    }
    String lang = LanguageGuesser.findSourceLanguageName(fenceLanguage);
    if (lang == null || settings.getDisabledInjectionsByLanguageAsList().contains(lang)) {
      return;
    }
    if (isInjectedByIdeLanguage(fenceLanguage)) {
      return;
    }
    TextRange range = host.getContentTextRange();
    if (range.equals(TextRange.EMPTY_RANGE)) {
      return;
    }
    String extension = findTextMateExtension(lang);
    if (extension == null) {
      return;
    }
    registrar.startInjecting(TextMateLanguage.LANGUAGE, extension);
    registrar.addPlace(null, null, host, range);
    registrar.doneInjecting();
  }

  private static boolean isInjectedByIdeLanguage(String fenceLanguage) {
    Language language = LanguageGuesser.guessLanguage(fenceLanguage);
    // some languages are known by name only, and they can't be injected
    return language != null && LanguageParserDefinitions.INSTANCE.forLanguage(language) != null;
  }

  @Nullable
  static String findTextMateExtension(@NotNull String lang) {
    String normalized = lang.toLowerCase(Locale.US);
    TextMateService service = TextMateService.getInstance();
    if (service.getLanguageDescriptorByExtension(normalized) != null) {
      return normalized;
    }
    // for example "python" is not an extension, but the scope name "source.python" tells that it is the grammar for "py"
    return service.getFileNameMatcherToScopeNameMapping().entrySet().stream()
      .filter(entry -> entry.getKey() instanceof TextMateFileNameMatcher.Extension)
      .filter(entry -> {
        String scope = entry.getValue().toString();
        return scope.equalsIgnoreCase("source." + normalized) || scope.equalsIgnoreCase("text." + normalized);
      })
      .map(Map.Entry::getKey)
      .map(matcher -> ((TextMateFileNameMatcher.Extension) matcher).getExtension())
      .min(Comparator.comparingInt(String::length))
      .orElse(null);
  }

  @NotNull
  @Override
  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Arrays.asList(AsciiDocListing.class, AsciiDocPassthrough.class, AsciiDocFrontmatter.class);
  }
}

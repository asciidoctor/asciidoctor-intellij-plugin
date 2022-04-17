package org.asciidoc.intellij.injection;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.lexer.EmbeddedTokenTypesProvider;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LanguageGuesser {

  private static List<EmbeddedTokenTypesProvider> embeddedTokenTypesProviders() {
    return Arrays.asList(EmbeddedTokenTypesProvider.EXTENSION_POINT_NAME.getExtensions());
  }

  private static Map<String, Language> langIdToLanguage() {
    final HashMap<String, Language> result = new HashMap<>();
    for (Language language : Language.getRegisteredLanguages()) {
      if (language.getID().isEmpty()) {
        continue;
      }
      if (!LanguageUtil.isInjectableLanguage(language)) {
        continue;
      }

      result.put(language.getID().toLowerCase(Locale.US).replaceAll(" ", ""), language);
    }

    final Language javascriptLanguage = result.get("javascript");
    if (javascriptLanguage != null) {
      result.put("js", javascriptLanguage);
    }

    if (!result.containsKey("bash")) {
      final Language l = result.get("shellscript");
      if (l != null) {
        result.put("bash", l);
      }
    }

    if (!result.containsKey("shell")) {
      final Language l = result.get("shellscript");
      if (l != null) {
        result.put("shell", l);
      }
    }

    if (!result.containsKey("jshell")) {
      final Language l = result.get("jshelllanguage");
      if (l != null) {
        result.put("jshell", l);
      }
    }

    return result;
  }

  @NotNull
  public static Map<String, Language> getLangToLanguageMap() {
    return Collections.unmodifiableMap(langIdToLanguage());
  }

  @Nullable
  public static Language guessLanguage(@NotNull String languageName) {
    String[] parts = languageName.split("-", -1);
    String lang = null;
    if ("source".equals(parts[0])) {
      lang = parts[1];
    } else if ("diagram-plantuml".equals(languageName)) {
      lang = "puml";
    } else if ("diagram-graphviz".equals(languageName)) {
      lang = "dot";
    }
    if (lang == null) {
      return null;
    }

    AsciiDocPreviewSettings settings = AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings();
    if (settings.getDisabledInjectionsByLanguageAsList().contains(lang)) {
      return null;
    }

    final Language languageFromMap = langIdToLanguage().get(lang.toLowerCase(Locale.US));
    if (languageFromMap != null) {
      return languageFromMap;
    }
    for (EmbeddedTokenTypesProvider provider : embeddedTokenTypesProviders()) {
      if (provider.getName().equalsIgnoreCase(languageName)) {
        return provider.getElementType().getLanguage();
      }
    }
    return null;
  }
}

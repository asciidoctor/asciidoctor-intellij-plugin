package org.asciidoc.intellij.injection;

import com.intellij.lang.Language;
import com.intellij.lexer.EmbeddedTokenTypesProvider;
import com.intellij.openapi.util.ClearableLazyValue;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum LanguageGuesser {
  INSTANCE;

  private final NotNullLazyValue<List<EmbeddedTokenTypesProvider>> embeddedTokenTypeProviders =
    new NotNullLazyValue<List<EmbeddedTokenTypesProvider>>() {
      @NotNull
      @Override
      protected List<EmbeddedTokenTypesProvider> compute() {
        return Arrays.asList(EmbeddedTokenTypesProvider.EXTENSION_POINT_NAME.getExtensions());
      }
    };

  private final NotNullLazyValue<Map<String, Language>> langIdToLanguage = new NotNullLazyValue<Map<String, Language>>() {
    @NotNull
    @Override
    protected Map<String, Language> compute() {
      final HashMap<String, Language> result = new HashMap<>();
      for (Language language : Language.getRegisteredLanguages()) {
        if (language.getID().isEmpty()) {
          continue;
        }

        result.put(language.getID().toLowerCase(Locale.US), language);
      }

      final Language javascriptLanguage = result.get("javascript");
      if (javascriptLanguage != null) {
        result.put("js", javascriptLanguage);
      }
      return result;
    }
  };

  @NotNull
  public Map<String, Language> getLangToLanguageMap() {
    return Collections.unmodifiableMap(langIdToLanguage.getValue());
  }

  @Nullable
  public Language guessLanguage(@NotNull String languageName) {
    String[] parts = languageName.split("-");
    String lang = null;
    if("source".equals(parts[0])) {
      lang = parts[1];
    } else if ("diagram-plantuml".equals(languageName)) {
      lang = "puml";
    } else if ("diagram-graphviz".equals(languageName)) {
      lang = "dot";
    }
    if(lang == null) {
      return null;
    }
    final Language languageFromMap = langIdToLanguage.getValue().get(lang.toLowerCase(Locale.US));
    if (languageFromMap != null) {
      return languageFromMap;
    }
    for (EmbeddedTokenTypesProvider provider : embeddedTokenTypeProviders.getValue()) {
      if (provider.getName().equalsIgnoreCase(languageName)) {
        return provider.getElementType().getLanguage();
      }
    }
    return null;
  }
}

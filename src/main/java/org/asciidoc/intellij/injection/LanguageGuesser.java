package org.asciidoc.intellij.injection;

import com.intellij.diagnostic.ImplementationConflictException;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.lexer.EmbeddedTokenTypesProvider;
import org.asciidoc.intellij.commandRunner.AsciiDocRunnerForPowershell;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
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

      // Handles for example "InjectablePHP" or "InjectedFreemarker"
      result.put(language.getID()
        .replaceAll("^Injectable", "")
        .replaceAll("^Injected", "")
        .toLowerCase(Locale.US)
        .replaceAll(" ", ""), language);
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

    if (!result.containsKey("csharp")) {
      final Language l = result.get("c#");
      if (l != null) {
        // this is the official name in highlight.js
        result.put("csharp", l);
        result.remove("c#");
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
    } else if ("diagram-salt".equals(languageName)) {
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

    /*
     Scripting languages for project setup and management are of course independent of their
     project language. Take for example Python, which is used to manage Java-Projects.
     Java-Projects use IntelliJ, which has no support for Python in the IDE itself,
     so the language won't be found in the classpath, although JetBrains has support for that
     langauge in general.
      */
    if (lang.equalsIgnoreCase("javascript")) {
      return findLanguage(JavaScriptLanguage.class);
    } else if (lang.equalsIgnoreCase("python")) {
      return findLanguage(PythonLanguage.class);
    } else if (AsciiDocRunnerForPowershell.isPowerShell(lang)) {
      return findLanguage(PowershellLanguage.class);
    } else if (lang.equalsIgnoreCase("ruby")) {
      return findLanguage(RubyLanguage.class);
    }

    return null;
  }

  private static <T extends Language> T findLanguage(Class<T> clazz) {
    final T instance = Language.findInstance(clazz);
    try {
      return instance != null ? instance : clazz.getDeclaredConstructor().newInstance();
    } catch (ImplementationConflictException e) {
      /* The language has already been registered. For unknown reasons the language may not be found
      via Language.findInstance(clazz), but already be registered. Then just try again. */
      return Language.findInstance(clazz);
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
      // This is controlled by us, there is definitely a no-parameter public constructor for all classes.
      throw new RuntimeException(e);
    }
  }

  private static class PythonLanguage extends Language {
    protected PythonLanguage() {
      super("python");
    }
  }

  private static class PowershellLanguage extends Language {
    protected PowershellLanguage() {
      super("powershell");
    }
  }

  private static class RubyLanguage extends Language {
    protected RubyLanguage() {
      super("ruby");
    }
  }

  private static class JavaScriptLanguage extends Language {
    protected JavaScriptLanguage() {
      super("javascript");
    }
  }

  private static class TypeScriptLanguage extends Language {
    protected TypeScriptLanguage() {
      super("typeScript");
    }
  }
}

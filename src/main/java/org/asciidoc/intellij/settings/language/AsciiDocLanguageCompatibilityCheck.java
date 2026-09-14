package org.asciidoc.intellij.settings.language;

import lombok.experimental.UtilityClass;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocRunnerForGo;
import org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocRunnerForJavaScript;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Find incompatible language parameters.
 */
@UtilityClass
class AsciiDocLanguageCompatibilityCheck {
  /**
   * Go address sanitizer.
   */
  private static final String ASAN = "-asan";
  /**
   * Go memory sanitizer.
   */
  private static final String MSAN = "-msan";

  /**
   * Find incompatible language parameters and returns a warning string if found.<br>
   * If no incompatibilities are found an "OK"-String gets returned.
   *
   * <p>This doesn't target to find <i>every</i> problematic combination,
   * but rather the most common ones. After all, this is for simple inline script executions,
   * not for replacing an IDE.
   *
   * @param selectedLanguage  Selected language.
   * @param parameters        Parameters.
   * @param alwaysUseTempFile Always use a temporary file.
   * @return A warning string if incompatibilities are found, otherwise an "OK" string.
   */
  @NonNull
  static String findIncompatibilities(int selectedLanguage, String parameters, boolean alwaysUseTempFile) {
    List<String> warnings = new ArrayList<>();
    switch (selectedLanguage) {
      case AsciiDocLanguages.Indices.INDEX_GO -> {
        if (containsWord(parameters, AsciiDocRunnerForGo.RACE)) {
          if (containsWord(parameters, ASAN)) {
            addIncompatibility(warnings, AsciiDocRunnerForGo.RACE, ASAN);
          }
          if (containsWord(parameters, MSAN)) {
            addIncompatibility(warnings, AsciiDocRunnerForGo.RACE, MSAN);
          }
        }
        if (containsWord(parameters, ASAN) && containsWord(parameters, MSAN)) {
          addIncompatibility(warnings, ASAN, MSAN);
        }
      }
      case AsciiDocLanguages.Indices.INDEX_JAVA_SCRIPT,
           AsciiDocLanguages.Indices.INDEX_TYPE_SCRIPT -> javaTypeScriptCheck(warnings, parameters, alwaysUseTempFile);
      case AsciiDocLanguages.Indices.INDEX_POWER_SHELL -> {
        if (containsWord(parameters, "-CommandWithArgs")) {
          // While it most likely will execute w/o red error messages, it won't work at all.
          warnings.add("Can't use -CommandWithArgs with this script execution");
        }
      }
      case AsciiDocLanguages.Indices.INDEX_PYTHON -> {
        if (containsWord(parameters, "-m")) {
          warnings.add("Can't use -m with this script execution, it will only execute the \"-m\"-module, nothing else.");
        }
      }
      case AsciiDocLanguages.Indices.INDEX_RUBY -> {
        // Ruby lets flags overwrite each other, but it doesn't abort.
      }
      default -> throw new IllegalStateException("Unexpected language index: " + selectedLanguage);
    }
    final String text;
    if (!warnings.isEmpty()) {
      text = "<html><font color='red'>Warning: " + String.join(" ", warnings) + "</font></html>";
    } else {
      text = "<html><font color='lime'>No <i>obvious</i> parameter conflicts.</font></html>";
    }
    return text;
  }

  private static void addIncompatibility(List<String> warnings, String first, String second) {
    warnings.add("Can't use %s with %s.".formatted(first, second));
  }

  private static void javaTypeScriptCheck(@NotNull List<String> warnings, String parameters, boolean alwaysUseTempFile) {
    if (alwaysUseTempFile && containsWord(parameters, AsciiDocRunnerForJavaScript.INPUT_TYPE_MODULE)) {
      addIncompatibility(warnings, AsciiDocRunnerForJavaScript.INPUT_TYPE_MODULE, "a temporary file");
    }
    if (containsWord(parameters, "-p")) {
      /* Even if it runs without red error messages in TypeScript, it doesn't make sense and won't work.
       * Here, scripts are either executed / evaluated via the "-e" parameter, or run via temporary file. */
      warnings.add("Can't use -p with this script execution.");
    }
  }

  private static boolean containsWord(String testString, String word) {
    return testString.matches("(?:.*\\s)?" + word + "(?:\\s.*)?");
  }
}

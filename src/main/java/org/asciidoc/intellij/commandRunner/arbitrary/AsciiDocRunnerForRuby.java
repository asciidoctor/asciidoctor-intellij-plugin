package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.util.SystemInfo;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.settings.language.AsciiDocScriptLanguageSetting;
import org.asciidoc.intellij.settings.language.AsciiDocScriptLanguageSettings;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.asciidoc.intellij.commandRunner.arbitrary.AsciiDocLanguageConstants.WIN_EXE_SUFFIX;

/**
 * Run adhoc Ruby code blocks in AsciiDoc documents.
 */
public class AsciiDocRunnerForRuby extends AsciiDocRunnerArbitrary {
  private static final String RUBY_LANGUAGE_ID = "ruby";

  private static final String RUBY_EXE = "ruby";

  private static String interpreterCache = null;

  @Override
  public boolean isApplicable(Language language) {
    return language != null //
      && language.getID().equalsIgnoreCase(RUBY_LANGUAGE_ID) //
      && hasInterpreter();
  }

  @Override
  @NotNull
  List<String> codeRunParameters(@Nullable AsciiDocScriptLanguageSetting languageSetting) {
    List<String> result = super.codeRunParameters(languageSetting);
    if (!useTemporaryFile(languageSetting)) {
      result.add("-e");
    }
    return result;
  }

  @Nullable
  @Override
  String findInterpreter() {
    if (interpreterCache != null) {
      return interpreterCache;
    }

    interpreterCache = findRubyInterpreter();
    return interpreterCache;
  }

  @Override
  @Nullable AsciiDocScriptLanguageSetting extractScriptLanguageSetting(
    AsciiDocScriptLanguageSettings languageSettings) {
    return languageSettings.getLanguageSettingRuby();
  }

  @Nullable
  public static String findRubyInterpreter() {
    // This will most likely only work in PyCharm.
    // Check Global SDKs for a Ruby interpreter.
    String sdkHomePath = findNativeInterpreter(RUBY_EXE);
    if (sdkHomePath != null) {
      return sdkHomePath;
    }

    // This should also work in IntelliJ.
    List<String> candidates = SystemInfo.isWindows ? List.of(RUBY_EXE + WIN_EXE_SUFFIX) : List.of(RUBY_EXE);

    for (String candidate : candidates) {
      if (canExecute(candidate)) {
        return candidate;
      }
    }

    return null;
  }

  private static boolean canExecute(String candidate) {
    // Allow full paths and commands resolved via PATH.
    Path path = Paths.get(candidate);
    if (path.isAbsolute()) {
      return path.toFile().canExecute();
    }

    File file = PathEnvironmentVariableUtil.findInPath(candidate);
    return file != null && file.canExecute();
  }

  @Nls
  @Override
  public String getTitle() {
    return AsciiDocBundle.message("asciidoc.runner.ruby");
  }

  @NotNull
  public static List<AsciiDocSuggestedParameter> suggestedParameters() {
    return List.of(//
      new AsciiDocSuggestedParameter("-w", "Turn on warnings.", null)
      //
    );
  }

  @Override
  @NotNull
  TempFileInfo getTempFileInfo() {
    return new TempFileInfo(".rb");
  }
}

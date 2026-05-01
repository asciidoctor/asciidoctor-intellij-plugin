package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.lang.Language;
import com.intellij.openapi.util.SystemInfo;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.settings.language.AsciiDocScriptLanguageSetting;
import org.asciidoc.intellij.settings.language.AsciiDocScriptLanguageSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Run adhoc Go code blocks in AsciiDoc documents.
 */
public class AsciiDocRunnerForGo extends AsciiDocRunnerArbitrary {

  private static final String WINDOWS_EXECUTABLE = "go.exe";
  private static final String UNIX_EXECUTABLE = "go";

  /**
   * Parameter for data race detection.
   */
  public static final String RACE = "-race";

  @Override
  String findInterpreter() {
    return findGoInterpreter();
  }

  @Override
  @Nullable AsciiDocScriptLanguageSetting extractScriptLanguageSetting(
    AsciiDocScriptLanguageSettings languageSettings) {
    return languageSettings.getLanguageSettingGo();
  }

  @Override
  public boolean isApplicable(@NotNull Language language) {
    String id = language.getID().toLowerCase(Locale.ROOT);
    String displayName = language.getDisplayName().toLowerCase(Locale.ROOT);
    return (isGo(id) || isGo(displayName)) && hasInterpreter();
  }

  @Override
  @NotNull
  List<String> codeRunParameters(@Nullable AsciiDocScriptLanguageSetting languageSetting) {
    var result = super.codeRunParameters(languageSetting);
    result.addFirst("run");
    return result;
  }

  @Override
  public String getTitle() {
    return AsciiDocBundle.message("asciidoc.runner.go");
  }

  public static boolean isGo(String value) {
    return value.equalsIgnoreCase("go");
  }

  @NotNull
  public static String findGoInterpreter() {
    return SystemInfo.isWindows ? WINDOWS_EXECUTABLE : UNIX_EXECUTABLE;
  }

  @NotNull
  public static List<AsciiDocSuggestedParameter> suggestedParameters() {
    return List.of(//
      new AsciiDocSuggestedParameter(RACE,
        "enable data race detection.", "1.1")
      //
    );
  }

  @Override
  boolean mustUseTemporaryFile() {
    return true;
  }

  @Override
  @NotNull
  TempFileInfo getTempFileInfo() {
    return new TempFileInfo(".go");
  }
}

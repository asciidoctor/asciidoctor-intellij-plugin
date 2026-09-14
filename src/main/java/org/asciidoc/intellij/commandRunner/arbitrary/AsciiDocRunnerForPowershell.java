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
 * Run adhoc PowerShell code blocks in AsciiDoc documents.
 */
public class AsciiDocRunnerForPowershell extends AsciiDocRunnerArbitrary {

  private static final String WINDOWS_EXECUTABLE = "powershell.exe";
  private static final String UNIX_EXECUTABLE = "pwsh";

  @Override
  String findInterpreter() {
    return findPowerShellInterpreter();
  }

  @Override
  @Nullable AsciiDocScriptLanguageSetting extractScriptLanguageSetting(
    AsciiDocScriptLanguageSettings languageSettings) {
    return languageSettings.getLanguageSettingPowerShell();
  }

  @Override
  public boolean isApplicable(@NotNull Language language) {
    String id = language.getID().toLowerCase(Locale.ROOT);
    String displayName = language.getDisplayName().toLowerCase(Locale.ROOT);
    return (isPowerShell(id) || isPowerShell(displayName)) && hasInterpreter();
  }

  @Override
  @NotNull
  List<String> codeRunParameters(@Nullable AsciiDocScriptLanguageSetting languageSetting) {
    List<String> result = super.codeRunParameters(languageSetting);
    if (!useTemporaryFile(languageSetting)) {
      result.add("-c");
    }
    return result;
  }

  @Override
  public String getTitle() {
    return AsciiDocBundle.message("asciidoc.runner.powershell");
  }

  public static boolean isPowerShell(String value) {
    return value.equalsIgnoreCase("powershell")
      || value.equalsIgnoreCase("pwsh")
      || value.equalsIgnoreCase("ps1")
      || value.equalsIgnoreCase("posh")
      || value.equalsIgnoreCase("power shell");
  }

  @NotNull
  public static String findPowerShellInterpreter() {
    return SystemInfo.isWindows ? WINDOWS_EXECUTABLE : UNIX_EXECUTABLE;
  }

  @NotNull
  public static List<AsciiDocSuggestedParameter> suggestedParameters() {
    return List.of(//
      new AsciiDocSuggestedParameter("-ExecutionPolicy Bypass",
        "Bypasses the script-blocking policy.", null)
      //
    );
  }

  @Override
  @NotNull
  TempFileInfo getTempFileInfo() {
    return new TempFileInfo(".ps1");
  }
}

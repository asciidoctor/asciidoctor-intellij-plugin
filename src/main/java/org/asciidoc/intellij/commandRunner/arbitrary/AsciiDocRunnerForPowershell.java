package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Locale;

public class AsciiDocRunnerForPowershell extends AsciiDocRunnerArbitrary {

  private static final String WINDOWS_EXECUTABLE = "powershell.exe";
  private static final String UNIX_EXECUTABLE = "pwsh";

  @Override
  public String findInterpreter(@NotNull Project project) {
    return SystemInfo.isWindows ? WINDOWS_EXECUTABLE : UNIX_EXECUTABLE;
  }

  @Override
  public boolean isApplicable(@NotNull Project project, @NotNull Language language) {
    String id = language.getID().toLowerCase(Locale.ROOT);
    String displayName = language.getDisplayName().toLowerCase(Locale.ROOT);
    return isPowerShell(id) || isPowerShell(displayName);
  }

  @NonNull
  @Override
  String codeRunParameter() {
    return "-c";
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
}

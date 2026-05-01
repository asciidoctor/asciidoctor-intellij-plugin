package org.asciidoc.intellij.commandRunner.arbitrary;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
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
 * Run adhoc Python code blocks in AsciiDoc documents.
 */
public class AsciiDocRunnerForPython extends AsciiDocRunnerArbitrary {
  private static final Logger LOG = Logger.getInstance(AsciiDocRunnerForPython.class);

  private static final String PYTHON_LANGUAGE_ID = "python";

  private static final String PYTHON_EXE = "python";
  private static final String PYTHON3_EXE = "python3";

  private static String interpreterCache = null;

  @Override
  public boolean isApplicable(Language language) {
    return language != null //
      && language.getID().equalsIgnoreCase(PYTHON_LANGUAGE_ID) //
      && hasInterpreter();
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

  @Nullable
  @Override
  String findInterpreter() {
    if (interpreterCache != null) {
      return interpreterCache;
    }

    interpreterCache = findPythonInterpreter();
    return interpreterCache;
  }

  @Override
  @Nullable AsciiDocScriptLanguageSetting extractScriptLanguageSetting(
    AsciiDocScriptLanguageSettings languageSettings) {
    return languageSettings.getLanguageSettingPython();
  }

  @Nullable
  public static String findPythonInterpreter() {
    // This will most likely only work in PyCharm.
    // Check Global SDKs for a Python interpreter.
    String sdkHomePath = findNativeInterpreter(PYTHON_EXE);
    if (sdkHomePath != null) {
      return sdkHomePath;
    }

    // This should also work in IntelliJ.
    List<String> candidates = SystemInfo.isWindows //
      ? List.of("py" + WIN_EXE_SUFFIX, PYTHON_EXE + WIN_EXE_SUFFIX, PYTHON3_EXE + WIN_EXE_SUFFIX) //
      : List.of(PYTHON3_EXE, PYTHON_EXE);

    for (String candidate : candidates) {
      LOG.debug("Candidate Python interpreter: " + candidate);
      if (canExecute(candidate)) {
        LOG.debug("Chosen Python interpreter: " + candidate);
        return candidate;
      }
    }
    return null;
  }

  private static boolean canExecute(String candidate) {
    // Allow full paths and commands resolved via PATH.
    Path path = Paths.get(candidate);
    if (path.isAbsolute()) {
      LOG.debug("Interpreter is absolute: " + path + ", can execute: " + path.toFile().canExecute());
      return path.toFile().canExecute();
    }
    LOG.debug("Got path for python: " + path + ".");

    File file = PathEnvironmentVariableUtil.findInPath(candidate);
    LOG.debug("Resolved path for candidate '" + candidate + "': " + file);
    return file != null && file.canExecute();
  }

  @Nls
  @Override
  public String getTitle() {
    return AsciiDocBundle.message("asciidoc.runner.python");
  }

  @NotNull
  public static List<AsciiDocSuggestedParameter> suggestedParameters() {
    return List.of(//
      new AsciiDocSuggestedParameter("-W ignore", "Ignore warnings.", null),
      new AsciiDocSuggestedParameter("-W error", "Treat every warning as an error.", null)
      //
    );
  }

  @Override
  @NotNull
  TempFileInfo getTempFileInfo() {
    return new TempFileInfo(".py");
  }
}

package org.asciidoc.intellij.commandRunner;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.SystemInfo;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class AsciiDocRunnerForPython extends AsciiDocRunnerArbitrary {
  private static final String PYTHON_LANGUAGE_ID = "python";

  private static final String PYTHON_EXE = "python";
  private static final String PYTHON3_EXE = "python3";

  private static String interpreterCache = null;

  @Override
  public boolean isApplicable(@NotNull Project project, Language language) {
    return language != null &&
      language.getID().equalsIgnoreCase(PYTHON_LANGUAGE_ID)
      && findInterpreter(project) != null;
  }

  @NonNull
  @Override
  String codeRunParameter() {
    return "-c";
  }

  @Nullable
  @Override
  String findInterpreter(Project project) {
    if (interpreterCache != null) {
      return interpreterCache;
    }
    // This will most likely only work in PyCharm.
    Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
    if (sdk != null && isLikelyPythonInterpreter(sdk.getHomePath())) {
      return sdk.getHomePath();
    }

    // This should also work in IntelliJ.
    List<String> candidates = SystemInfo.isWindows
      ? List.of("py", PYTHON_EXE, PYTHON3_EXE)
      : List.of(PYTHON3_EXE, PYTHON_EXE);

    for (String candidate : candidates) {
      if (canExecute(candidate)) {
        interpreterCache = candidate;
        return candidate;
      }
    }

    return null;
  }

  private static boolean isLikelyPythonInterpreter(@Nullable String homePath) {
    if (homePath == null || homePath.isBlank()) {
      return false;
    }

    String fileName = Paths.get(homePath).getFileName().toString().toLowerCase(Locale.ROOT);
    return fileName.startsWith(PYTHON_LANGUAGE_ID) || fileName.equals("py") || fileName.equals("py.exe");
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
    return AsciiDocBundle.message("asciidoc.runner.python");
  }
}

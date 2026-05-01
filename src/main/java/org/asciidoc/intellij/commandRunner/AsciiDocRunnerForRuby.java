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

public class AsciiDocRunnerForRuby extends AsciiDocRunnerArbitrary {
  private static final String RUBY_LANGUAGE_ID = "ruby";

  private static final String RUBY_EXE = "ruby";

  private static String interpreterCache = null;

  @Override
  public boolean isApplicable(@NotNull Project project, Language language) {
    return language != null &&
      language.getID().equalsIgnoreCase(RUBY_LANGUAGE_ID)
      && findInterpreter(project) != null;
  }

  @NonNull
  @Override
  String codeRunParameter() {
    return "-e";
  }

  @Nullable
  @Override
  String findInterpreter(Project project) {
    if (interpreterCache != null) {
      return interpreterCache;
    }
    // This will most likely only work in RubyMine.
    Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
    if (sdk != null && isLikelyRubyInterpreter(sdk.getHomePath())) {
      return sdk.getHomePath();
    }

    // This should also work in IntelliJ.
    List<String> candidates = SystemInfo.isWindows
      ? List.of(RUBY_EXE + ".exe", RUBY_EXE)
      : List.of(RUBY_EXE);

    for (String candidate : candidates) {
      if (canExecute(candidate)) {
        interpreterCache = candidate;
        return candidate;
      }
    }

    return null;
  }

  private static boolean isLikelyRubyInterpreter(@Nullable String homePath) {
    if (homePath == null || homePath.isBlank()) {
      return false;
    }

    String fileName = Paths.get(homePath).getFileName().toString().toLowerCase(Locale.ROOT);
    return fileName.startsWith(RUBY_LANGUAGE_ID) || fileName.equals("ruby.exe");
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
}

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

public class AsciiDocRunnerForJavaScript extends AsciiDocRunnerArbitrary {
  private static final String JAVA_SCRIPT_LANGUAGE_ID = "javascript";

  private static String interpreterCache = null;

  @Override
  public boolean isApplicable(@NotNull Project project, Language language) {
    return language != null &&
      language.getID().equalsIgnoreCase(JAVA_SCRIPT_LANGUAGE_ID)
      && findInterpreter(project) != null;
  }

  @Override
  @NonNull String codeRunParameter() {
    return "-e";
  }

  @Nls
  @Override
  public String getTitle() {
    return AsciiDocBundle.message("asciidoc.runner.javascript");
  }

  @Nullable
  @Override
  protected String findInterpreter(@NotNull Project project) {
    if (interpreterCache != null) {
      return interpreterCache;
    }
    // Check the project SDK for a Node.js interpreter
    Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
    if (projectSdk != null) {
      String sdkHomePath = projectSdk.getHomePath();
      if (sdkHomePath != null) {
        String node = SystemInfo.isWindows ? "node.exe" : "node";
        Path candidate = Paths.get(sdkHomePath, node);
        if (candidate.toFile().canExecute()) {
          interpreterCache = candidate.toString();
          return interpreterCache;
        }
      }
    }
    // Fall back to PATH
    String[] candidates = SystemInfo.isWindows
      ? new String[]{"node.exe"}
      : new String[]{"node", "nodejs"};
    for (String candidate : candidates) {
      File file = PathEnvironmentVariableUtil.findInPath(candidate);
      if (file != null) {
        interpreterCache = file.getAbsolutePath();
        return interpreterCache;
      }
    }
    return null;
  }
}

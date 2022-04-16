package org.asciidoc.intellij.commandRunner;

import com.intellij.execution.Executor;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.sh.ShLanguage;
import com.intellij.sh.run.ShRunner;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.Nls;

public class AsciiDocRunnerForSh implements AsciiDocRunner {
  @Override
  public boolean isApplicable(Language language) {
    return language != null && language.is(ShLanguage.INSTANCE);
  }

  @Override
  public boolean run(String command, Project project, VirtualFile virtualFile, Executor executor) {
    ShRunner shRunner = ApplicationManager.getApplication().getService(ShRunner.class);
    String workingDirectory = virtualFile.getParent().getPath();
    if (SystemInfoRt.isWindows) {
      workingDirectory = workingDirectory.replace("/", "\\") + "\\";
    }
    if (shRunner != null && shRunner.isAvailable(project)) {
      shRunner.run(project, command, workingDirectory, AsciiDocBundle.message("asciidoc.runner.snippet"), true);
    }
    return true;
  }

  @Nls
  @Override
  public String getTitle() {
    return AsciiDocBundle.message("asciidoc.runner.sh");
  }
}

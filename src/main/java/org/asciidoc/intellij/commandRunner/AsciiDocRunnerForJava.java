package org.asciidoc.intellij.commandRunner;

import com.intellij.execution.Executor;
import com.intellij.execution.jshell.JShellDiagnostic;
import com.intellij.execution.jshell.JShellHandler;
import com.intellij.execution.jshell.protocol.Response;
import com.intellij.lang.Language;
import com.intellij.lang.java.JShellLanguage;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsciiDocRunnerForJava implements AsciiDocRunner {
  @Override
  public boolean isApplicable(@NotNull Project project, Language language) {
    return language != null &&
      (language.is(JavaLanguage.INSTANCE) || language.is(JShellLanguage.INSTANCE));
  }

  @Override
  public boolean run(String command, Project project, VirtualFile virtualFile, Executor executor) {
    final JShellHandler handler = JShellHandler.getAssociatedHandler(virtualFile);
    // Ensure this runs on the UI thread.
    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        JShellHandler finalHandler;
        if (handler == null) {
          finalHandler = JShellHandler.create(project, virtualFile, null, null);
        } else {
          finalHandler = handler;
        }
        finalHandler.toFront();
        Future<Response> evaluate = finalHandler.evaluate(command.trim());
        if (evaluate != null) {
          evaluate.get(1, TimeUnit.SECONDS);
        }
      } catch (TimeoutException ex) {
        // noop
      } catch (Exception ex) {
        JShellDiagnostic.notifyError(ex, project);
      }
    });
    return true;
  }

  @Nls
  @Override
  public String getTitle() {
    return AsciiDocBundle.message("asciidoc.runner.java");
  }
}

package org.asciidoc.intellij.commandRunner;

import com.intellij.execution.Executor;
import com.intellij.execution.jshell.JShellDiagnostic;
import com.intellij.execution.jshell.JShellHandler;
import com.intellij.execution.jshell.protocol.Response;
import com.intellij.lang.Language;
import com.intellij.lang.java.JShellLanguage;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.asciidoc.intellij.AsciiDocBundle;
import org.jetbrains.annotations.Nls;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsciiDocRunnerForJava implements AsciiDocRunner {
  @Override
  public boolean isApplicable(Language language) {
    return language != null &&
      (language.is(JavaLanguage.INSTANCE) || language.is(JShellLanguage.INSTANCE));
  }

  @Override
  public boolean run(String command, Project project, VirtualFile virtualFile, Executor executor) {
    try {
      JShellHandler handler = JShellHandler.getAssociatedHandler(virtualFile);
      if (handler == null) {
        handler = JShellHandler.create(project, virtualFile, null, null);
      }
      handler.toFront();
      Future<Response> evaluate = handler.evaluate(command.trim());
      if (evaluate != null) {
        evaluate.get(1, TimeUnit.SECONDS);
      }
    } catch (TimeoutException ex) {
      // noop
    } catch (Exception ex) {
      JShellDiagnostic.notifyError(ex, project);
    }
    return true;
  }

  @Nls
  @Override
  public String getTitle() {
    return AsciiDocBundle.message("asciidoc.runner.java");
  }
}

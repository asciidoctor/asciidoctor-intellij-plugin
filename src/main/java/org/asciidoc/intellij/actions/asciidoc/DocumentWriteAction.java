package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;

public class DocumentWriteAction {

  public static void run(final Project project, Runnable runnable, String name) {
    ApplicationManager.getApplication().invokeLater(() ->
      CommandProcessor.getInstance().executeCommand(project,
        () -> ApplicationManager.getApplication().runWriteAction(runnable), name, null));
  }
}

package org.asciidoc.intellij.util;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;

/**
 * Created by erikp on 05/12/14.
 */
public class PluginUtils {

  /**
   * Execute an action inside a <code>CommandProcessor.executeCommand()</code> and
   * <code>ApplicationManager.runWriteAction()</code> command.
   *
   * @param action  Action object triggered by event
   * @param event   Event object triggered by user
   * @param command Runnable object to execute action
   */
  public static void executeWriteAction(AnActionEvent action, AnActionEvent event, final Runnable command) {
    final Project project = event.getData(DataKeys.PROJECT);
    CommandProcessor.getInstance().executeCommand(
        project,
        new Runnable() {
          public void run() {
            try {
              ApplicationManager.getApplication().runWriteAction(command);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        },
        "name",
        "description"
    );
  }

}

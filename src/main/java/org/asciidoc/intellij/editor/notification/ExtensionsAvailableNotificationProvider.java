package org.asciidoc.intellij.editor.notification;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.BaseProjectDirectories;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

public class ExtensionsAvailableNotificationProvider implements EditorNotificationProvider, DumbAware {

  @Override
  public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return null;
    }
    String bp = null;

    projectsLoop:
    for (Project p : ProjectManager.getInstance().getOpenProjects()) {
      for (VirtualFile d : BaseProjectDirectories.getBaseDirectories(p)) {
        if (file.getPath().startsWith(d.getPath())) {
          bp = p.getBasePath();
          break projectsLoop;
        }
      }
    }

    if (bp == null) {
      return null;
    }
    final String projectBasePath = bp;
    final AsciiDocApplicationSettings asciiDocApplicationSettings = AsciiDocApplicationSettings.getInstance();
    if (asciiDocApplicationSettings.getExtensionsEnabled(project, projectBasePath) != null
      || !Boolean.TRUE.equals(asciiDocApplicationSettings.getExtensionsPresent(project, projectBasePath))) {
      return null;
    }
    return fileEditor -> {
      EditorNotificationPanel panel = new EditorNotificationPanel();
      panel.setText("This project contains Asciidoctor Extensions in .asciidoctor/lib that can be executed to render the preview. As this Ruby code runs as your local user, don't run this code unchecked.");
      panel.createActionLabel("I've checked the code and trust it, enable them!", () -> {
        asciiDocApplicationSettings.setExtensionsEnabled(projectBasePath, true);
        EditorNotifications.updateAll();
      });
      panel.createActionLabel("Don't enable them!", () -> {
        asciiDocApplicationSettings.setExtensionsEnabled(projectBasePath, false);
        EditorNotifications.updateAll();
      });
      return panel;
    };
  }
}

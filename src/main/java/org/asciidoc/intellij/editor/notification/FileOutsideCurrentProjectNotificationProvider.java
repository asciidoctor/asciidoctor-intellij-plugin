package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

/**
 * Notify user that they opened a file outside of the current project and that functionality is limited.
 */
public class FileOutsideCurrentProjectNotificationProvider implements EditorNotificationProvider, DumbAware {
  private static final String FILEOUTSIDEFOLDER = "asciidoc.fileoutsidefolder";

  @Override
  public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
    // only in AsciiDoc files
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return null;
    }

    if (LightEdit.owns(project)) {
      return null;
    }

    // only if not previously disabled
    if (PropertiesComponent.getInstance().getBoolean(FILEOUTSIDEFOLDER)) {
      return null;
    }

    String fileName = file.getCanonicalPath();
    if (fileName == null) {
      return null;
    }

    for (VirtualFile root : AsciiDocUtil.getRoots(project)) {
      if (fileName.startsWith(root.getPath())) {
        return null;
      }
    }

    return fileEditor -> {
      final EditorNotificationPanel panel = new EditorNotificationPanel();
      panel.setText("The file you're editing is outside of the current project's folder. Auto-complete and validation is limited.");
      panel.createActionLabel("Tell me more!", ()
              -> BrowserUtil.browse("https://intellij-asciidoc-plugin.ahus1.de/docs/users-guide/faq/validation-for-asciidoc-files.html"));
      panel.createActionLabel("Do not show again", () -> {
        PropertiesComponent.getInstance().setValue(FILEOUTSIDEFOLDER, true);
        EditorNotifications.updateAll();
      });
      return panel;
    };
  }
}

package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Notify user that they opened a file outside of the current project and that functionality is limited.
 */
public class FileOutsideCurrentProjectNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("File outside current project");

  private static final String FILEOUTSIDEFOLDER = "asciidoc.fileoutsidefolder";

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull final FileEditor fileEditor, @NotNull Project project) {
    // only in AsciiDoc files
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
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

    for (String root : AsciiDocUtil.getRoots(project)) {
      if (fileName.startsWith(root)) {
        return null;
      }
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("The file you're editing is outside of the current project's folder. Auto-complete and validation is limited.");
    panel.createActionLabel("Tell me more!", ()
      -> BrowserUtil.browse("https://intellij-asciidoc-plugin.ahus1.de/docs/users-guide/faq/validation-for-asciidoc-files.html"));
    panel.createActionLabel("Do not show again", () -> {
      PropertiesComponent.getInstance().setValue(FILEOUTSIDEFOLDER, true);
      EditorNotifications.updateAll();
    });
    return panel;
  }
}

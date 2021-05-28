package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Notify user that they should update the IntelliJ version.
 *
 * @author Alexander Schwartz (alexander.schwartz@gmx.net) 2021
 */
public class UpdateIntellijNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("Please update IntelliJ");

  private static final String PLEASE_UPDATE_2021_1_1 = "asciidoc.intellij.pleaseupdate.2021.1.1";

  private static final String PLEASE_UPDATE_2021_1_2 = "asciidoc.intellij.pleaseupdate.2021.1.2";

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

    final ApplicationInfo applicationInfo = ApplicationInfo.getInstance();

    if (SystemInfoRt.isMac
      && (applicationInfo.getFullVersion().equals("2021.1") || applicationInfo.getFullVersion().equals("2021.1.1"))
      && !PropertiesComponent.getInstance().getBoolean(PLEASE_UPDATE_2021_1_2)) {
      final EditorNotificationPanel panel = new EditorNotificationPanel();
      panel.setText("The IDE versions 2021.1 and 2021.1.1 on macOS can sometimes freeze. Please update to version 2021.1.2!");
      panel.createActionLabel("Yes, tell me more!", ()
        -> BrowserUtil.browse("https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/765"));
      panel.createActionLabel("Do not show again", () -> {
        PropertiesComponent.getInstance().setValue(PLEASE_UPDATE_2021_1_2, true);
        EditorNotifications.updateAll();
      });
      return panel;
    }

    if (applicationInfo.getFullVersion().equals("2021.1")
       && !PropertiesComponent.getInstance().getBoolean(PLEASE_UPDATE_2021_1_1)) {
      final EditorNotificationPanel panel = new EditorNotificationPanel();
      panel.setText("The IDE version 2021.1 has a known indexing bug. Please update to version 2021.1.1!");
      panel.createActionLabel("Yes, tell me more!", ()
        -> BrowserUtil.browse("https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/725"));
      panel.createActionLabel("Do not show again", () -> {
        PropertiesComponent.getInstance().setValue(PLEASE_UPDATE_2021_1_1, true);
        EditorNotifications.updateAll();
      });
      return panel;
    }

    return null;
  }
}

package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.PatternUtil;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.ide.actions.ShowSettingsUtilImpl.showSettingsDialog;

/**
 * Notify user that permanent softwrap is available.
 */
public class EnableSoftWrapNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("Enable Softwrap for Asciidoctor");

  private static final String SOFTWRAP_AVAILABLE = "asciidoc.softwrap.enable";

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
    if (PropertiesComponent.getInstance().getBoolean(SOFTWRAP_AVAILABLE)) {
      return null;
    }

    // don't show if soft wrap is already enabled by default for this file
    if (isSoftWrapEnabledByDefaultForFile(file)) {
      return null;
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("Writing AsciiDoc works best with soft-wrap enabled. Do you want to enable it by default?");
    panel.createActionLabel("Yes, take me to the Soft Wrap settings!", () -> ApplicationManager.getApplication().invokeLater(() -> {
      if (!project.isDisposed()) {
        showSettingsDialog(project, "preferences.editor", "soft wraps");
      }
    }));
    panel.createActionLabel("Do not show again", () -> {
      PropertiesComponent.getInstance().setValue(SOFTWRAP_AVAILABLE, true);
      EditorNotifications.updateAll();
    });
    return panel;
  }

  /**
   * Check if the file given as parameter would open as soft-wrapped by default.
   * Logic is inspired by {@link com.intellij.openapi.editor.impl.SettingsImpl#isUseSoftWraps()}.
   */
  private boolean isSoftWrapEnabledByDefaultForFile(VirtualFile file) {
    boolean softWrapsEnabled = EditorSettingsExternalizable.getInstance().isUseSoftWraps();
    if (!softWrapsEnabled) {
      return false;
    }

    String masks = EditorSettingsExternalizable.getInstance().getSoftWrapFileMasks();
    if (masks.trim().equals("*")) {
      return true;
    }

    return file != null && fileNameMatches(file.getName(), masks);
  }

  private static boolean fileNameMatches(@NotNull String fileName, @NotNull String globPatterns) {
    for (String p : globPatterns.split(";", -1)) {
      String pTrimmed = p.trim();
      if (!pTrimmed.isEmpty() && PatternUtil.fromMask(pTrimmed).matcher(fileName).matches()) {
        return true;
      }
    }
    return false;
  }

}

package org.asciidoc.intellij.editor.notification;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

import static com.intellij.ide.actions.ShowSettingsUtilImpl.showSettingsDialog;

/**
 * Notify user that permanent softwrap is available.
 */
public class EnableSoftWrapNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("Enable Softwrap for Asciidoctor");
  public static final int LEVEL_TO_TRIGGER_NOTIFICATION = 5;

  private static final String SOFTWRAP_AVAILABLE = "asciidoc.softwrap.enable";
  private static AtomicInteger netActivations = new AtomicInteger();

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  public static void toggle(boolean state) {
    // count number of net activations so we can show a notification later.
    if (state) {
      netActivations.incrementAndGet();
    } else {
      netActivations.decrementAndGet();
    }
    if (netActivations.get() > EnableSoftWrapNotificationProvider.LEVEL_TO_TRIGGER_NOTIFICATION) {
      EditorNotifications.updateAll();
    }
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

    // find out if toggle has been activated multiple times
    if (netActivations.get() <= LEVEL_TO_TRIGGER_NOTIFICATION) {
      return null;
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("You enabled Soft Wrap multiple times. Do you want to enable it permanently?");
    panel.createActionLabel("Yes, take me to the settings (then scroll down)!", ()
      -> {
      netActivations.set(0);
      EditorNotifications.updateAll();
      showSettingsDialog(project, "preferences.editor", "soft wraps");
    });
    panel.createActionLabel("Do not show again", () -> {
      PropertiesComponent.getInstance().setValue(SOFTWRAP_AVAILABLE, true);
      EditorNotifications.updateAll();
    });
    return panel;
  }
}

package org.asciidoc.intellij.editor.javafx;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaFxInitializationIsStuckNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("JavaFxInitializationIsStuck");

  private static final String DONT_NOTIFY_STUCK_JAVAFX = "asciidoc.do.not.notify.about.stuck.javafx";

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull final FileEditor fileEditor) {
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return null;
    }
    if (PropertiesComponent.getInstance().getBoolean(DONT_NOTIFY_STUCK_JAVAFX)) {
      return null;
    }
    if (!(new JavaFxHtmlPanelProvider().isJavaFxStuck())) {
      return null;
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("JavaFX initialization is stuck. Please look at your IDE logs for JavaFX error messages.");
    panel.createActionLabel("Do not show again", () -> {
      PropertiesComponent.getInstance().setValue(DONT_NOTIFY_STUCK_JAVAFX, true);
      EditorNotifications.updateAll();
    });
    return panel;
  }
}

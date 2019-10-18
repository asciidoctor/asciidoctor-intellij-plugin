package org.asciidoc.intellij.editor.javafx.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChangeJdkForJavaFXNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("JDK should be changed to support JavaFX");
  private JavaFx javaFx;
  private PreviewNotificationRepository previewNotification;

  //Used for reflection based construction
  public ChangeJdkForJavaFXNotificationProvider() {
    javaFx = new JavaFx();
    previewNotification = new PreviewNotificationRepository();
  }

  protected   ChangeJdkForJavaFXNotificationProvider(JavaFx javaFx, PreviewNotificationRepository previewNotification) {
    this.javaFx = javaFx;
    this.previewNotification = previewNotification;
  }

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

    if (!previewNotification.isShown()) {
      return null;
    }

    if (!javaFx.isCurrentHtmlProvider(getAsciiDocApplicationSettings())) {
      return null;
    }

    if (javaFx.isAvailable()) {
      return null;
    }

    if (javaFx.isStuck()) {
      // there is a different notification about a stuck JavaFX initialization; don't show this notification now
      return null;
    }

    return notificationPanelFactory(getAsciiDocApplicationSettings());
  }

  @NotNull
  protected EditorNotificationPanel notificationPanelFactory(AsciiDocApplicationSettings asciiDocApplicationSettings) {
    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("You could enable the advanced JavaFX preview if you would change to JetBrains 64bit JDK.");
    panel.createActionLabel("Yes, tell me more!", ()
      -> BrowserUtil.browse("https://github.com/asciidoctor/asciidoctor-intellij-plugin/wiki/JavaFX-preview"));
    panel.createActionLabel("Do not show again", () -> {
      previewNotification.reset();
      EditorNotifications.updateAll();
    });
    return panel;
  }

  @NotNull
  protected AsciiDocApplicationSettings getAsciiDocApplicationSettings() {
    return AsciiDocApplicationSettings.getInstance();
  }
}

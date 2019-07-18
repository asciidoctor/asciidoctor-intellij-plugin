package org.asciidoc.intellij.editor.javafx;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChangeJdkForJavaFXNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("JDK should be changed to support JavaFX");

  private static final String DONT_ASK_TO_CHANGE_JDK_FOR_JAVAFX = "asciidoc.do.not.ask.to.change.jdk.for.javafx";

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
    if (PropertiesComponent.getInstance().getBoolean(DONT_ASK_TO_CHANGE_JDK_FOR_JAVAFX)) {
      return null;
    }
    final AsciiDocApplicationSettings asciiDocApplicationSettings = AsciiDocApplicationSettings.getInstance();
    final AsciiDocPreviewSettings oldPreviewSettings = asciiDocApplicationSettings.getAsciiDocPreviewSettings();
    if (oldPreviewSettings.getHtmlPanelProviderInfo().getClassName().equals(JavaFxHtmlPanelProvider.class.getName())) {
      return null;
    }
    final AsciiDocHtmlPanelProvider.AvailabilityInfo availabilityInfo = new JavaFxHtmlPanelProvider().isAvailable();
    if (availabilityInfo == AsciiDocHtmlPanelProvider.AvailabilityInfo.AVAILABLE) {
      return null;
    }
    if (new JavaFxHtmlPanelProvider().isJavaFxStuck()) {
      // there is a different notification about a stuck JavaFX initialization; don't show this notification now
      return null;
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("You could enable the advanced JavaFX preview if you would change to JetBrains 64bit JDK.");
    panel.createActionLabel("Do not show again", () -> {
      PropertiesComponent.getInstance().setValue(DONT_ASK_TO_CHANGE_JDK_FOR_JAVAFX, true);
      EditorNotifications.updateAll();
    });
    return panel;
  }
}

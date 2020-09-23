package org.asciidoc.intellij.editor.javafx.notification;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.editor.jcef.AsciiDocJCEFHtmlPanelProvider;
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
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull final FileEditor fileEditor, @NotNull Project project) {
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return null;
    }
    if (PropertiesComponent.getInstance().getBoolean(DONT_ASK_TO_CHANGE_JDK_FOR_JAVAFX)) {
      return null;
    }
    final AsciiDocApplicationSettings asciiDocApplicationSettings = AsciiDocApplicationSettings.getInstance();
    final AsciiDocPreviewSettings previewSettings = asciiDocApplicationSettings.getAsciiDocPreviewSettings();
    if (previewSettings.getHtmlPanelProviderInfo().getClassName().equals(JavaFxHtmlPanelProvider.class.getName())) {
      return null;
    }
    final AsciiDocHtmlPanelProvider.AvailabilityInfo javafxAvailabilityInfo = new JavaFxHtmlPanelProvider().isAvailable();
    if (javafxAvailabilityInfo == AsciiDocHtmlPanelProvider.AvailabilityInfo.AVAILABLE) {
      return null;
    }
    final AsciiDocHtmlPanelProvider.AvailabilityInfo jcefAvailabilityInfo = new AsciiDocJCEFHtmlPanelProvider().isAvailable();
    if (jcefAvailabilityInfo == AsciiDocHtmlPanelProvider.AvailabilityInfo.AVAILABLE) {
      return null;
    }
    if (new JavaFxHtmlPanelProvider().isJavaFxStuck()) {
      // there is a different notification about a stuck JavaFX initialization; don't show this notification now
      return null;
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("You could enable the advanced JavaFX preview if you would change to JetBrains 64bit JDK with JavaFX support.");
    panel.createActionLabel("Yes, tell me more!", ()
      -> BrowserUtil.browse("https://intellij-asciidoc-plugin.ahus1.de/docs/users-guide/features/preview/javafx-preview.html"));
    panel.createActionLabel("Do not show again", () -> {
      PropertiesComponent.getInstance().setValue(DONT_ASK_TO_CHANGE_JDK_FOR_JAVAFX, true);
      EditorNotifications.updateAll();
    });
    return panel;
  }
}

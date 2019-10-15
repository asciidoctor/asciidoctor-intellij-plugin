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
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChangeJdkForJavaFXNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("JDK should be changed to support JavaFX");
  private JavaFx javaFx;

  //Used for reflection based construction
  public ChangeJdkForJavaFXNotificationProvider() {
    javaFx = new JavaFx();
  }

  protected   ChangeJdkForJavaFXNotificationProvider(JavaFx javaFx) {
    this.javaFx = javaFx;
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

    final AsciiDocApplicationSettings asciiDocApplicationSettings = getAsciiDocApplicationSettings();
    if (!asciiDocApplicationSettings.isShowJavaFxPreviewInstructions()) {
      return null;
    }

    if (!javaFx.isCurrentHtmlProvider(asciiDocApplicationSettings)) {
      return null;
    }

    if (javaFx.isAvailable()) {
      return null;
    }

    if (javaFx.isStuck()) {
      // there is a different notification about a stuck JavaFX initialization; don't show this notification now
      return null;
    }

    return notificationPanelFactory(asciiDocApplicationSettings);
  }

  @NotNull
  protected EditorNotificationPanel notificationPanelFactory(AsciiDocApplicationSettings asciiDocApplicationSettings) {
    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("You could enable the advanced JavaFX preview if you would change to JetBrains 64bit JDK.");
    panel.createActionLabel("Yes, tell me more!", ()
      -> BrowserUtil.browse("https://github.com/asciidoctor/asciidoctor-intellij-plugin/wiki/JavaFX-preview"));
    panel.createActionLabel("Do not show again", () -> {
      AsciiDocPreviewSettings asciiDocPreviewSettings = asciiDocApplicationSettings.getAsciiDocPreviewSettings();
      asciiDocApplicationSettings.setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(
        asciiDocPreviewSettings.getSplitEditorLayout(),
        asciiDocPreviewSettings.getHtmlPanelProviderInfo(),
        asciiDocPreviewSettings.getPreviewTheme(),
        asciiDocPreviewSettings.getSafeMode(),
        asciiDocPreviewSettings.getAttributes(),
        asciiDocPreviewSettings.isVerticalSplit(),
        asciiDocPreviewSettings.isEditorFirst(),
        asciiDocPreviewSettings.isEnabledInjections(),
        asciiDocPreviewSettings.getDisabledInjectionsByLanguage(),
        asciiDocPreviewSettings.isShowAsciiDocWarningsAndErrorsInEditor(),
        asciiDocPreviewSettings.isInplacePreviewRefresh(),
        asciiDocPreviewSettings.isKrokiEnabled(),
        asciiDocPreviewSettings.getKrokiUrl(), false));
      EditorNotifications.updateAll();
    });
    return panel;
  }

  @NotNull
  protected AsciiDocApplicationSettings getAsciiDocApplicationSettings() {
    return AsciiDocApplicationSettings.getInstance();
  }
}

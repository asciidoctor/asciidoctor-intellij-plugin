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
import com.intellij.ui.scale.JBUIScale;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This notification shows how to fix the symptoms of a blurry preview.
 * Ticket: https://youtrack.jetbrains.com/issue/IDEA-213110
 */
public class JavaFxMightBeBlurredNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("Preview might be blurry");

  private static final String ASCIIDOC_PREVIEW_MIGHT_BE_BLURRY = "asciidoc.preview.might.be.blurry";

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
    if (PropertiesComponent.getInstance().getBoolean(ASCIIDOC_PREVIEW_MIGHT_BE_BLURRY)) {
      return null;
    }

    // only if JavaFX preview is active
    final AsciiDocApplicationSettings asciiDocApplicationSettings = AsciiDocApplicationSettings.getInstance();
    final AsciiDocPreviewSettings oldPreviewSettings = asciiDocApplicationSettings.getAsciiDocPreviewSettings();
    if (!oldPreviewSettings.getHtmlPanelProviderInfo().getClassName().equals(JavaFxHtmlPanelProvider.class.getName())) {
      return null;
    }

    // don't show if workaround for blurred preview active
    if ("false".equalsIgnoreCase(System.getProperty("sun.java2d.uiScale.enabled"))) {
      return null;
    }

    String message;

    switch (System.getProperty("os.name")) {
      case "Windows 10":
        // seems to be problem with JDK8, and fixed on JRE11+
        if (!"1.8".equals(System.getProperty("java.specification.version"))) {
          return null;
        }

        // if there is no fraction, don't show message
        float winScale = JBUIScale.sysScale();
        float fraction = winScale - (int) winScale;
        if (fraction < 0.05f || fraction > 0.95f) {
          return null;
        }

        message = "Fractional scale detected on Windows/JBR8, JavaFX preview could be blurry.";

        break;

      case "Linux":
        float linScale = JBUIScale.sysScale();
        if (linScale > 0.95f && linScale < 1.05f) {
          // no scaling on Linux, don't show message
          return null;
        }

        message = "Scaled display detected on Linux, JavaFX preview could be blurry.";

        break;

      default:
        return null;
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText(message);
    panel.createActionLabel("Yes, the preview is blurry, show me how to fix it!", ()
      -> BrowserUtil.browse("https://github.com/asciidoctor/asciidoctor-intellij-plugin/wiki/Blurry-preview"));
    panel.createActionLabel("Do not show again", () -> {
      PropertiesComponent.getInstance().setValue(ASCIIDOC_PREVIEW_MIGHT_BE_BLURRY, true);
      EditorNotifications.updateAll();
    });
    return panel;
  }
}

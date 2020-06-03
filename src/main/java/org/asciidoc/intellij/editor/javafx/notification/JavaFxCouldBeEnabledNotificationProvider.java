package org.asciidoc.intellij.editor.javafx.notification;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
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

public class JavaFxCouldBeEnabledNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  private static final Key<EditorNotificationPanel> KEY = Key.create("AsciiDoc JavaFX Preview Could Be Enabled");

  private static final String DONT_ASK_TO_CHANGE_PROVIDER_TYPE_KEY = "asciidoc.do.not.ask.to.change.preview.provider";

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
    if (PropertiesComponent.getInstance().getBoolean(DONT_ASK_TO_CHANGE_PROVIDER_TYPE_KEY)) {
      return null;
    }
    final AsciiDocApplicationSettings asciiDocApplicationSettings = AsciiDocApplicationSettings.getInstance();
    final AsciiDocPreviewSettings oldPreviewSettings = asciiDocApplicationSettings.getAsciiDocPreviewSettings();
    if (oldPreviewSettings.getHtmlPanelProviderInfo().getClassName().equals(JavaFxHtmlPanelProvider.class.getName())) {
      return null;
    }
    final AsciiDocHtmlPanelProvider.AvailabilityInfo jcefAvailabilityInfo = new AsciiDocJCEFHtmlPanelProvider().isAvailable();
    if (jcefAvailabilityInfo == AsciiDocHtmlPanelProvider.AvailabilityInfo.AVAILABLE) {
      return null;
    }

    final AsciiDocHtmlPanelProvider.AvailabilityInfo availabilityInfo = new JavaFxHtmlPanelProvider().isAvailable();
    if (availabilityInfo == AsciiDocHtmlPanelProvider.AvailabilityInfo.UNAVAILABLE) {
      return null;
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("JavaFX WebKit-based preview renderer is available.");
    panel.createActionLabel("Change preview browser to JavaFX", () -> {
      final boolean isSuccess = availabilityInfo.checkAvailability(panel);
      if (isSuccess) {
        asciiDocApplicationSettings.setAsciiDocPreviewSettings(new AsciiDocPreviewSettings(
          oldPreviewSettings.getSplitEditorLayout(),
          new JavaFxHtmlPanelProvider().getProviderInfo(),
          oldPreviewSettings.getPreviewTheme(),
          oldPreviewSettings.getSafeMode(),
          oldPreviewSettings.getAttributes(),
          oldPreviewSettings.isVerticalSplit(),
          oldPreviewSettings.isEditorFirst(),
          oldPreviewSettings.isEnabledInjections(),
          oldPreviewSettings.getLanguageForPassthrough(),
          oldPreviewSettings.getDisabledInjectionsByLanguage(),
          oldPreviewSettings.isShowAsciiDocWarningsAndErrorsInEditor(),
          oldPreviewSettings.isInplacePreviewRefresh(),
          oldPreviewSettings.isKrokiEnabled(),
          oldPreviewSettings.getKrokiUrl(),
          oldPreviewSettings.isAttributeFoldingEnabled(),
          oldPreviewSettings.getZoom(),
          oldPreviewSettings.isHideErrorsInSourceBlocks(),
          oldPreviewSettings.getHideErrorsByLanguage()));
        EditorNotifications.updateAll();
      } else {
        Logger.getInstance(JavaFxCouldBeEnabledNotificationProvider.class).warn("Could not install and apply OpenJFX");
      }
    });
    panel.createActionLabel("Do not show again", () -> {
      PropertiesComponent.getInstance().setValue(DONT_ASK_TO_CHANGE_PROVIDER_TYPE_KEY, true);
      EditorNotifications.updateAll();
    });
    return panel;
  }
}

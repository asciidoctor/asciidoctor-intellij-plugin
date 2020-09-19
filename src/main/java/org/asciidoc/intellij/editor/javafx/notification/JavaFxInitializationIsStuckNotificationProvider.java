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
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This notified the user that JavaFX initialization failed.
 * Initial handling was done here: https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/299
 * I've reported this as part of https://github.com/JetBrains/intellij-community/pull/1178.
 * Apparently this should be fixed in 2019.2 with JBR 11. I still see the chance that people run an old version of the IDE or
 * a non-JBR JRE, therefore keep it in here for now.
 */
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
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull final FileEditor fileEditor, @NotNull Project project) {
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return null;
    }
    if (PropertiesComponent.getInstance().getBoolean(DONT_NOTIFY_STUCK_JAVAFX)) {
      return null;
    }
    if (!new JavaFxHtmlPanelProvider().isJavaFxStuck()) {
      return null;
    }

    final EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText("JavaFX initialization is stuck. Falling back to Swing preview until this is resolved.");
    panel.createActionLabel("Show me how to fix it!", ()
      -> BrowserUtil.browse("https://intellij-asciidoc-plugin.ahus1.de/docs/users-guide/faq/javafx-initialization-stuck.html"));
    panel.createActionLabel("Do not show again", () -> {
      PropertiesComponent.getInstance().setValue(DONT_NOTIFY_STUCK_JAVAFX, true);
      EditorNotifications.updateAll();
    });
    return panel;
  }
}

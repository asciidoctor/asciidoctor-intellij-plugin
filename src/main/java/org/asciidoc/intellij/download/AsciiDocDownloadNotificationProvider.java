package org.asciidoc.intellij.download;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Function;

public class AsciiDocDownloadNotificationProvider implements EditorNotificationProvider, DumbAware {
  private static final com.intellij.openapi.diagnostic.Logger LOG =
    com.intellij.openapi.diagnostic.Logger.getInstance(AsciiDocDownloadNotificationProvider.class);

  private static volatile boolean notification = false;

  public static void showNotification() {
    if (!notification) {
      notification = true;
      ApplicationManager.getApplication().invokeLater(EditorNotifications::updateAll);
    }
  }

  public static void hideNotification() {
    if (notification) {
      notification = false;
      ApplicationManager.getApplication().invokeLater(EditorNotifications::updateAll);
    }
  }

  @Override
  public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
    if (file.getFileType() != AsciiDocFileType.INSTANCE) {
      return null;
    }
    if (!notification) {
      return null;
    }
    if (AsciiDocDownloaderUtil.downloadCompleteAsciidoctorJDiagram()) {
      return null;
    }
    return fileEditor -> {
      EditorNotificationPanel panel = new EditorNotificationPanel();
      panel.setText("To be able to show diagrams in the preview, download asciidoctorj-diagram or enable Kroki in the settings.");
      panel.createActionLabel("Yes, download now!", ()
              -> {
        notification = false;
        EditorNotifications.updateAll();
        AsciiDocDownloaderUtil.downloadAsciidoctorJDiagram(project, () -> {
          Notifications.Bus
                  .notify(new Notification("asciidoctor", AsciiDocBundle.message("asciidoc.download.title"),
                          AsciiDocBundle.message("asciidoc.download.asciidoctorj-diagram.success"),
                          NotificationType.INFORMATION));
          notification = false;
          EditorNotifications.updateAll();
        }, (e) -> {
          LOG.warn("unable to download", e);
          notification = true;
          EditorNotifications.updateAll();
        });
      });
      panel.createActionLabel("Hide", () -> {
        notification = false;
        EditorNotifications.updateAll();
      });
      return panel;
    };
  }
}

package org.asciidoc.intellij;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;
import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;

/**
 * @author Alexander Schwartz 2018
 */
public class IntellijLogHandler implements LogHandler {

  private final String rootFile;

  public IntellijLogHandler(String rootFile) {
    this.rootFile = rootFile;
  }

  @Override
  public void log(LogRecord logRecord) {
    String file = null;
    if (logRecord.getCursor() != null) {
      file = logRecord.getCursor().getFile();
    }
    if (file == null) {
      file = rootFile;
    }
    NotificationType notificationType;
    switch (logRecord.getSeverity()) {
      case ERROR:
      case FATAL:
        notificationType = NotificationType.ERROR;
        break;
      case WARN:
        notificationType = NotificationType.WARNING;
        break;
      default:
        notificationType = NotificationType.INFORMATION;
    }
    StringBuilder message = new StringBuilder();
    message.append(logRecord.getSeverity().name()).append(" ");
    message.append(logRecord.getSourceFileName()).append(":").append(logRecord.getSourceMethodName()).append(": ");
    if (logRecord.getCursor() != null && logRecord.getCursor().getFile() != null) {
      message.append(logRecord.getCursor().getFile()).append(":").append(logRecord.getCursor().getLineNumber());
    }
    message.append(" ").append(logRecord.getMessage());
    Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
      .createNotification("Message during rendering " + file, message.toString(), notificationType, null);
    notification.setImportant(notificationType != NotificationType.INFORMATION);

    if (logRecord.getMessage().startsWith("allow-uri-read is not enabled; cannot embed remote image")) {
      notification.addAction(NotificationAction.createSimpleExpiring(
        "Set 'allow-uri-read' property", this::setAllowUriRead));
    }

    if (logRecord.getMessage().startsWith("problem encountered in image")
      && logRecord.getSourceFileName().endsWith("asciidoctor/pdf/converter.rb")
      && logRecord.getMessage().contains("Invalid attributes on tag rect; skipping tag")) {
      notification.addAction(NotificationAction.createSimpleExpiring(
        "Set 'allow-uri-read' property", this::setAllowUriRead));
    }

    Notifications.Bus.notify(notification);
  }

  private void setAllowUriRead() {
    AsciiDocApplicationSettings instance = AsciiDocApplicationSettings.getInstance();
    AsciiDocPreviewSettings asciiDocPreviewSettings = instance.getAsciiDocPreviewSettings();
    asciiDocPreviewSettings.getAttributes().put("allow-uri-read", "");
    instance.setAsciiDocPreviewSettings(asciiDocPreviewSettings);

    Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
      .createNotification("Configuration changed", "The 'allow-uri-read' property is now set. Please retry.", NotificationType.INFORMATION, null);
    notification.setImportant(false);
    Notifications.Bus.notify(notification);
  }
}

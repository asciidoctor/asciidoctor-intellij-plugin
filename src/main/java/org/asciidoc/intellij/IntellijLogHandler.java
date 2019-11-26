package org.asciidoc.intellij;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
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
    String file = logRecord.getCursor().getFile();
    if (file == null) {
      file = rootFile;
    }
    NotificationType notificationType;
    switch (logRecord.getSeverity()) {
      case ERROR:
        notificationType = NotificationType.ERROR;
        break;
      case WARN:
        notificationType = NotificationType.WARNING;
        break;
      case FATAL:
        notificationType = NotificationType.ERROR;
        break;
      default:
        notificationType = NotificationType.INFORMATION;
    }
    Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
      .createNotification("Message during rendering " + file, logRecord.getSourceFileName() + ":"
        + logRecord.getSourceMethodName() + ": " + logRecord.getMessage(), notificationType, null);
    notification.setImportant(false);
    Notifications.Bus.notify(notification);
  }
}

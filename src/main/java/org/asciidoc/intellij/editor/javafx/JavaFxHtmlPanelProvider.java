package org.asciidoc.intellij.editor.javafx;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.editor.Document;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.asciidoc.intellij.editor.jeditor.JeditorHtmlPanelProvider;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.file.Path;

public class JavaFxHtmlPanelProvider extends AsciiDocHtmlPanelProvider {
  public static final ProviderInfo INFO = new ProviderInfo("JavaFX WebView", JavaFxHtmlPanelProvider.class.getName());

  private static boolean initialized;

  /**
   * Initialization might fail if a different StreamHandlerFactory has already been registered.
   * Ask here if this is the case.
   */
  public static boolean isInitialized() {
    return initialized;
  }

  static {
    try {
      URL.setURLStreamHandlerFactory(new LocalfileURLStreamHandlerFactory());
      initialized = true;
    } catch (Error error) {
      initialized = false;
      Notification notification = AsciiDocPreviewEditor.NOTIFICATION_GROUP
        .createNotification("Message during initialization", "Can't register URLStreamHandlerFactory, " +
          "reloading of images in JavaFX preview will not work. Possible root cause: a conflicting plugin " +
          "is installed (currently 'Fabric for Android Studio' is one known conflict).", NotificationType.WARNING, null);
      notification.setImportant(false);
      Notifications.Bus.notify(notification);
    }
  }

  @NotNull
  @Override
  public AsciiDocHtmlPanel createHtmlPanel(Document document, Path imagesPath) {
    return new JavaFxHtmlPanel(document, imagesPath);
  }

  @NotNull
  @Override
  public AvailabilityInfo isAvailable() {
    /* trying to determine 64bit platforms, due to problem with OpenJDK x86 on Windows */
    String architecture = System.getProperty("os.arch");
    if (!architecture.equals("amd64") // Windows und Linux amd64 = 64bit
        && !architecture.equals("x86_64") // Mac Intel x86_64 = 64bit
        ) {
      return AvailabilityInfo.UNAVAILABLE;
    }

    try {
      if (Class.forName("javafx.scene.web.WebView", false, getClass().getClassLoader()) != null) {
        return AvailabilityInfo.AVAILABLE;
      }
    }
    catch (ClassNotFoundException ignored) {
    }

    return AvailabilityInfo.UNAVAILABLE;
  }

  @NotNull
  @Override
  public ProviderInfo getProviderInfo() {
    return INFO;
  }

}

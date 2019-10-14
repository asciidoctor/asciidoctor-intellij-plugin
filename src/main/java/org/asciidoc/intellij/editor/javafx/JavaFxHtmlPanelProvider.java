package org.asciidoc.intellij.editor.javafx;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.sun.javafx.application.PlatformImpl;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.editor.AsciiDocPreviewEditor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaFxHtmlPanelProvider extends AsciiDocHtmlPanelProvider {
  public static final String JAVA_FX_WEB_VIEW_NAME = "JavaFX WebView";
  public static final ProviderInfo INFO = new ProviderInfo(JAVA_FX_WEB_VIEW_NAME, JavaFxHtmlPanelProvider.class.getName());
  private Logger log = Logger.getInstance(JavaFxHtmlPanelProvider.class);
  private static final AtomicBoolean HAS_WAITED = new AtomicBoolean(false);

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
        if (isJavaFxStuck()) {
          return AvailabilityInfo.UNAVAILABLE;
        }
        return AvailabilityInfo.AVAILABLE;
      }
    } catch (ClassNotFoundException ignored) {
    }

    return AvailabilityInfo.UNAVAILABLE;
  }

  public boolean isJavaFxStuck() {
    try {
      Field startupLatchField = PlatformImpl.class.getDeclaredField("startupLatch");
      startupLatchField.setAccessible(true);
      CountDownLatch startupLatch = (CountDownLatch) startupLatchField.get(null);

      Field initializedField = PlatformImpl.class.getDeclaredField("initialized");
      initializedField.setAccessible(true);
      AtomicBoolean initialized = (AtomicBoolean) initializedField.get(null);

      if (startupLatch.getCount() == 1 && initialized.get()) {
        if (!HAS_WAITED.get()) {
          // wait a bit to allow initialization to finish, but only once
          try {
            startupLatch.await(5, TimeUnit.SECONDS);
          } catch (InterruptedException ignored) {
          }
          HAS_WAITED.set(true);
        }
        if (startupLatch.getCount() == 1) {
          log.warn("JavaFX is stuck");
          // previous initialization failed, JavaFX not available
          return true;
        }
      }
    } catch (NoSuchFieldException | IllegalAccessException ex) {
      log.error("can't read state of PlatformImpl", ex);
    }
    return false;
  }

  @NotNull
  @Override
  public ProviderInfo getProviderInfo() {
    return INFO;
  }

}

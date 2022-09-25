package org.asciidoc.intellij.editor.javafx;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.sun.javafx.application.PlatformImpl;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaFxHtmlPanelProvider extends AsciiDocHtmlPanelProvider {
  public static final ProviderInfo INFO = new ProviderInfo("JavaFX WebView", JavaFxHtmlPanelProvider.class.getName());
  private static final Logger LOG = Logger.getInstance(JavaFxHtmlPanelProvider.class);
  private static final AtomicBoolean HAS_WAITED = new AtomicBoolean(false);

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
      // will throw ClassNotFoundException when JavaFX is not available, which is the case in:
      // * some non-JetBrains JDKs
      // * the latest JetBrains JDKs unless JavaFX plugin is installed
      Class.forName("javafx.scene.web.WebView", false, getClass().getClassLoader());
      if (isJavaFxStuck()) {
        return AvailabilityInfo.UNAVAILABLE;
      }
      return AvailabilityInfo.AVAILABLE;
    } catch (ClassNotFoundException ignored) {
      LOG.debug("no JavaFX found");
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
          } catch (InterruptedException ex) {
            LOG.warn("interrupted while waiting", ex);
          }
          HAS_WAITED.set(true);
        }
        if (startupLatch.getCount() == 1) {
          LOG.warn("JavaFX is stuck");
          // previous initialization failed, JavaFX not available
          return true;
        }
      }
    } catch (NoClassDefFoundError ex) {
      LOG.debug("can't find class PlatformImpl", ex);
    } catch (NoSuchFieldException | IllegalAccessException ex) {
      LOG.error("can't read state of PlatformImpl", ex);
    } catch (IllegalAccessError e) {
      // ignore - might not work due to a module access problem (seen in JDK 17)
    }
    return false;
  }

  @NotNull
  @Override
  public ProviderInfo getProviderInfo() {
    return INFO;
  }

}

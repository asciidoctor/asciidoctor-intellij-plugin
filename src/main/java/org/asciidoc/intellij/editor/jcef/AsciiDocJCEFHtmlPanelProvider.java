package org.asciidoc.intellij.editor.jcef;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.ui.jcef.JBCefApp;
import org.asciidoc.intellij.download.AsciiDocDownloaderUtil;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class AsciiDocJCEFHtmlPanelProvider extends AsciiDocHtmlPanelProvider {
  public static final ProviderInfo INFO = new ProviderInfo("JCEF Browser", AsciiDocJCEFHtmlPanelProvider.class.getName());

  private static final Logger LOG = Logger.getInstance(AsciiDocDownloaderUtil.class);

  @NotNull
  @Override
  public AsciiDocHtmlPanel createHtmlPanel(Document document, Path imagesPath, Runnable forceRefresh) {
    clearStaleJCEFlock();
    return new AsciiDocJCEFHtmlPanel(document, imagesPath, forceRefresh);
  }

  private static synchronized void clearStaleJCEFlock() {
    // Workaround for https://youtrack.jetbrains.com/issue/IJPL-148653,
    // when a lock file remains which the name of the old host name
    // Run this check everytime an AsciiDoc editor is opened to prevent problems, for example, after a computer comes back from suspend mode.
    Path jcefCache = PathManager.getSystemDir().resolve("jcef_cache");
    if (jcefCache.toFile().exists()) {
      Path singletonLock = jcefCache.resolve("SingletonLock");
      try {
        String hostName = InetAddress.getLocalHost().getHostName();
        String lockFile = Files.readSymbolicLink(singletonLock).toFile().getName();
        if (!lockFile.matches(Pattern.quote(hostName) + "-[0-9]*")) {
          // delete the stale lock to prevent the preview from appearing blank
          Files.delete(singletonLock);
        }
      } catch (NoSuchFileException ignore) {
        // nop
      } catch (IOException e) {
        LOG.warn("Can't check lock", e);
      }
    }
  }

  @NotNull
  @Override
  public AvailabilityInfo isAvailable() {
    return JBCefApp.isSupported() ? AvailabilityInfo.AVAILABLE : AvailabilityInfo.UNAVAILABLE;
  }

  @NotNull
  @Override
  public ProviderInfo getProviderInfo() {
    return INFO;
  }

}

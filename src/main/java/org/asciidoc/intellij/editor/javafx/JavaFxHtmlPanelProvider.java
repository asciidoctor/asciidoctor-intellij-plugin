package org.asciidoc.intellij.editor.javafx;

import com.intellij.openapi.editor.Document;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.file.Path;

public class JavaFxHtmlPanelProvider extends AsciiDocHtmlPanelProvider {

  static {
    URL.setURLStreamHandlerFactory(new LocalfileURLStreamHandlerFactory());
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
    return new ProviderInfo("JavaFX WebView", JavaFxHtmlPanelProvider.class.getName());
  }

}

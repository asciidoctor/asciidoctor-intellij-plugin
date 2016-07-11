package org.asciidoc.intellij.editor.javafx;

import com.intellij.openapi.editor.Document;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

public class JavaFxHtmlPanelProvider extends AsciiDocHtmlPanelProvider {

  static {
    URL.setURLStreamHandlerFactory(new LocalfileURLStreamHandlerFactory());
  }

  @NotNull
  @Override
  public AsciiDocHtmlPanel createHtmlPanel(Document document) {
    return new JavaFxHtmlPanel(document);
  }

  @NotNull
  @Override
  public AvailabilityInfo isAvailable() {
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

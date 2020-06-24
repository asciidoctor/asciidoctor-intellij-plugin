package org.asciidoc.intellij.editor.jcef;

import com.intellij.openapi.editor.Document;
import com.intellij.ui.jcef.JBCefApp;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class AsciiDocJCEFHtmlPanelProvider extends AsciiDocHtmlPanelProvider {
  public static final ProviderInfo INFO = new ProviderInfo("JCEF Browser", AsciiDocJCEFHtmlPanelProvider.class.getName());

  @NotNull
  @Override
  public AsciiDocHtmlPanel createHtmlPanel(Document document, Path imagesPath) {
    return new AsciiDocJCEFHtmlPanel(document, imagesPath);
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

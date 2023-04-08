package org.asciidoc.intellij.editor.jeditor;

import com.intellij.openapi.editor.Document;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class JeditorHtmlPanelProvider extends AsciiDocHtmlPanelProvider {
  public static final ProviderInfo INFO = new ProviderInfo("Swing", JeditorHtmlPanelProvider.class.getName());

  @NotNull
  @Override
  public AsciiDocHtmlPanel createHtmlPanel(Document document, Path imagesPath, Runnable forceRefresh) {
    return new JeditorHtmlPanel(document);
  }

  @NotNull
  @Override
  public AvailabilityInfo isAvailable() {
    return AvailabilityInfo.AVAILABLE;
  }

  @NotNull
  @Override
  public ProviderInfo getProviderInfo() {
    return INFO;
  }
}

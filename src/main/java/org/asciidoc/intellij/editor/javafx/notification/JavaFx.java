package org.asciidoc.intellij.editor.javafx.notification;

import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.asciidoc.intellij.settings.AsciiDocPreviewSettings;

public class JavaFx {

  private final JavaFxHtmlPanelProvider javaFxHtmlPanelProvider;

  JavaFx() {
    javaFxHtmlPanelProvider = new JavaFxHtmlPanelProvider();
  }

  public boolean isAvailable() {
    return javaFxHtmlPanelProvider.isAvailable() == AsciiDocHtmlPanelProvider.AvailabilityInfo.AVAILABLE;
  }

  public boolean isStuck() {
    return javaFxHtmlPanelProvider.isJavaFxStuck();
  }

  public boolean isCurrentHtmlProvider(AsciiDocApplicationSettings asciiDocApplicationSettings) {
    AsciiDocPreviewSettings asciiDocPreviewSettings = asciiDocApplicationSettings.getAsciiDocPreviewSettings();
    AsciiDocHtmlPanelProvider.ProviderInfo currentHtmlPanelProviderInfo = asciiDocPreviewSettings.getHtmlPanelProviderInfo();
    return currentHtmlPanelProviderInfo.equals(javaFxHtmlPanelProvider.getProviderInfo());
  }
}

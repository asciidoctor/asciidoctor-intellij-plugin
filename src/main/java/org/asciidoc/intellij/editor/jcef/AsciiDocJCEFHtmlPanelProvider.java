package org.asciidoc.intellij.editor.jcef;

import com.intellij.openapi.editor.Document;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.util.net.ProxySettings;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanel;
import org.asciidoc.intellij.editor.AsciiDocHtmlPanelProvider;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class AsciiDocJCEFHtmlPanelProvider extends AsciiDocHtmlPanelProvider {
  public static final ProviderInfo INFO = new ProviderInfo("JCEF Browser", AsciiDocJCEFHtmlPanelProvider.class.getName());

  @NotNull
  @Override
  public AsciiDocHtmlPanel createHtmlPanel(Document document, Path imagesPath, Runnable forceRefresh) {
    // The first JBCefApp access reads the proxy settings inside its class initializer (JBCefApp$Holder.<clinit>).
    // This is the first access to the proxy settings in the IDE, which initializes the internal ProxyMigrationService
    // there, and the platform reports "Class initialization must not depend on services" (Sentry ASCIIDOC-INTELLIJ-PLUGIN-B8F),
    // for IntelliJ 2026.2.
    // Reading them here, outside a class initializer, initializes those services beforehand.
    // As of 2026-11, this is still open as https://youtrack.jetbrains.com/issue/IJPL-212214 and the platform's master branch
    // still reads them in the class initializer.
    ProxySettings.getInstance().getProxyConfiguration();
    return new AsciiDocJCEFHtmlPanel(document, imagesPath, forceRefresh);
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

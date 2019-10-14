package org.asciidoc.intellij.editor;

import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider.JAVA_FX_WEB_VIEW_NAME;

public class AsciiDocHtmlPanelProviderCompanion {
  private final List<AsciiDocHtmlPanelProvider> providers;

  public AsciiDocHtmlPanelProviderCompanion(AsciiDocHtmlPanelProvider[] providers) {
    this.providers = new ArrayList<>(Arrays.asList(providers));
  }

  public AsciiDocHtmlPanelProvider[] invoke() {
    if (doesNotContainsJavaFX()) {
      addJavaFXToProviders();
    }

    return providers.toArray(new AsciiDocHtmlPanelProvider[0]);
  }

  private void addJavaFXToProviders() {
    providers.add(new JavaFxHtmlPanelProvider());
  }


  private  boolean doesNotContainsJavaFX() {
    for (AsciiDocHtmlPanelProvider provider: providers) {
      if (JAVA_FX_WEB_VIEW_NAME.equals(provider.getProviderInfo().getName())) {
        return false;
      }
    }

    return true;
  }

}

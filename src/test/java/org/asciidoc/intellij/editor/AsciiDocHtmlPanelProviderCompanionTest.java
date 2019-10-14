package org.asciidoc.intellij.editor;

import org.asciidoc.intellij.editor.javafx.JavaFxHtmlPanelProvider;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AsciiDocHtmlPanelProviderCompanionTest {

  @Test
  public void whenProvidersContainsJavaFXNoneIsAdded() {
    AsciiDocHtmlPanelProvider[] providers = new AsciiDocHtmlPanelProvider[]{new JavaFxHtmlPanelProvider()};
    AsciiDocHtmlPanelProviderCompanion panelProviderXXX = new AsciiDocHtmlPanelProviderCompanion(providers);

    AsciiDocHtmlPanelProvider[] panelProviders = panelProviderXXX.invoke();

    assertArrayEquals(panelProviders, panelProviders);
  }

  @Test
  public void whenProvidersDoesNotContainJavaFXThenIsAdded() {
    AsciiDocHtmlPanelProvider[] expectedProviders = new AsciiDocHtmlPanelProvider[]{new JavaFxHtmlPanelProvider()};
    AsciiDocHtmlPanelProvider[] providers = new AsciiDocHtmlPanelProvider[]{};
    AsciiDocHtmlPanelProviderCompanion panelProviderXXX = new AsciiDocHtmlPanelProviderCompanion(providers);

    AsciiDocHtmlPanelProvider[] panelProviders = panelProviderXXX.invoke();

    assertEquals(1, panelProviders.length);
    assertEquals(expectedProviders[0].getProviderInfo(), panelProviders[0].getProviderInfo());
  }

}

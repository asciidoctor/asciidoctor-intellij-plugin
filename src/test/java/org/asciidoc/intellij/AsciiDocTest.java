package org.asciidoc.intellij;

import java.io.File;

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.junit.Assert;

/**
 * @author Alexander Schwartz 2018
 */
public class AsciiDocTest extends LightPlatformCodeInsightFixtureTestCase {

  private AsciiDoc asciidoc;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    asciidoc = new AsciiDoc(new File("."), null, "test");
  }

  public void testShouldRenderPlantUmlAsPng() {
    String html = asciidoc.render("[plantuml,test,format=svg]\n" +
      "----\n" +
      "List <|.. ArrayList\n" +
      "----\n");
    Assert.assertTrue(html.contains("src=\"test.png\""));
  }

  public void testShouldRenderPlainAsciidoc() {
    String html = asciidoc.render("this is *bold*.");
    System.out.println(html);
    Assert.assertTrue(html.contains("<strong>bold</strong>"));
  }

  public void testShouldRenderAttributesAsciidoc() {
    String expectedContent = "should replace attribute placeholder with value";
    AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getAttributes().put("attr", expectedContent);
    String html = asciidoc.render("{attr}");
    Assert.assertTrue(html.contains(expectedContent));
  }
}

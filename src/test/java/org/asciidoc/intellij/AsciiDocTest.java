package org.asciidoc.intellij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.junit.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Alexander Schwartz 2018
 */
public class AsciiDocTest extends LightPlatformCodeInsightFixtureTestCase {
  private Logger log = Logger.getInstance(AsciiDocTest.class);

  private AsciiDoc asciidoc;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    asciidoc = new AsciiDoc(".", new File("."), null, "test");
  }

  public void testShouldRenderPlantUmlAsPng() {
    String html = asciidoc.render("[plantuml,test,format=svg]\n" +
      "----\n" +
      "List <|.. ArrayList\n" +
      "----\n", Collections.emptyList());
    Assert.assertTrue(html.contains("src=\"test.png\""));
  }

  public void testShouldRenderPlainAsciidoc() {
    String html = asciidoc.render("this is *bold*.", Collections.emptyList());
    Assert.assertTrue(html.contains("<strong>bold</strong>"));
  }

  public void testShouldRenderPdf() throws IOException {
    // given...
    File asciidoc = File.createTempFile("asciidocforapdf", ".adoc");
    File pdf = new File(asciidoc.getAbsoluteFile().getAbsolutePath().replaceAll("\\.adoc$", ".pdf"));
    try {
      Assert.assertTrue("replacemante should have worked", pdf.getName().endsWith(".pdf"));
      if (pdf.exists()) {
        fail("PDF already exists, but shouldn't before runnning AsciiDoc");
      }
      FileWriter fw = new FileWriter(asciidoc);
      fw.write("Hello world.");
      fw.close();

      // when...
      this.asciidoc.renderPdf(asciidoc, "", new ArrayList<>());

      // then...
      Assert.assertTrue(pdf.exists());

    } finally {
      // cleanup...
      if (asciidoc.exists()) {
        if (!asciidoc.delete()) {
          log.warn("unable to delete source file");
        }
      }
      if (pdf.exists()) {
        if (!pdf.delete()) {
          log.warn("unable to delete destination file");
        }
      }
    }
  }

  public void testShouldRenderAttributesAsciidoc() {
    String expectedContent = "should replace attribute placeholder with value";
    AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().getAttributes().put("attr", expectedContent);
    String html = asciidoc.render("{attr}", Collections.emptyList());
    Assert.assertTrue(html.contains(expectedContent));
  }
}

package org.asciidoc.intellij;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * @author Alexander Schwartz 2018
 */
public class AsciiDocTest {

  private AsciiDoc asciidoc;

  @Before
  public void setup() {
    asciidoc = new AsciiDoc(new File("."), null, "test");
  }

  @Test
  public void shouldRenderPlantUmlAsPng() {
    String html = asciidoc.render("[plantuml,test,format=svg]\n" +
      "----\n" +
      "List <|.. ArrayList\n" +
      "----\n");
    Assert.assertTrue(html.contains("src=\"test.png\""));
  }

  @Test
  public void shouldRenderPlainAsciidoc() {
    String html = asciidoc.render("this is *bold*.");
    System.out.println(html);
    Assert.assertTrue(html.contains("<strong>bold</strong>"));
  }
}

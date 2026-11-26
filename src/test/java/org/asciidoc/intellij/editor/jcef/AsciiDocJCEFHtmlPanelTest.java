package org.asciidoc.intellij.editor.jcef;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AsciiDocJCEFHtmlPanelTest {

  @Test
  public void shouldDeclareUtf8AsFirstElementInHead() {
    String html = AsciiDocJCEFHtmlPanel.addMetaTags("<html><head></head><body>─</body></html>", "default-src 'self'; img-src $x");
    assertThat(html).startsWith("<html><head>\n<meta charset=\"UTF-8\">\n<meta http-equiv=\"Content-Security-Policy\"");
    assertThat(html).contains("content=\"default-src 'self'; img-src $x\"");
  }

  @Test
  public void shouldNotChangeHtmlWithoutHead() {
    assertThat(AsciiDocJCEFHtmlPanel.addMetaTags("<body></body>", "x")).isEqualTo("<body></body>");
  }
}

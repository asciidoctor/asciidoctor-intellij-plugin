package org.asciidoc.intellij.actions;

import org.junit.Test;
import org.pegdown.ParsingTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ConvertToAsciiDocActionTest {

  private static String largeMarkdown() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 3000; i++) {
      sb.append("It's paragraph ").append(i).append(" with *emphasis* and `code` and a [link](http://example.com).\n\n");
    }
    return sb.toString();
  }

  @Test
  public void shouldConvertMarkdown() {
    String result = ConvertToAsciiDocAction.convertMarkdownToAsciiDoc("# Title\n\nSome *text*.\n", 60_000);
    assertTrue(result, result.startsWith("= Title"));
  }

  @Test
  public void shouldFailWithTimeoutIfParsingTakesTooLong() {
    String markdown = largeMarkdown();
    RuntimeException ex = assertThrows(RuntimeException.class, () -> ConvertToAsciiDocAction.convertMarkdownToAsciiDoc(markdown, 1));
    assertTrue(ConvertToAsciiDocAction.isTimeout(ex));
  }

  @Test
  public void shouldDetectTimeoutOnlyForPegdownTimeouts() {
    assertTrue(ConvertToAsciiDocAction.isTimeout(new RuntimeException(new ParsingTimeoutException())));
    assertFalse(ConvertToAsciiDocAction.isTimeout(new RuntimeException(new IllegalStateException())));
    assertEquals(0, ConvertToAsciiDocAction.convertMarkdownToAsciiDoc("", 60_000).length());
  }
}

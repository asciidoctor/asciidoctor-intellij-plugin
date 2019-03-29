package org.asciidoc.intellij.annotator;

import com.intellij.lang.annotation.HighlightSeverity;
import org.junit.Test;

import static org.junit.Assert.*;

public class AsciidocAnnotationResultTypeTest {

  @Test
  public void shouldAddAMessageWithLine() {
    String message = "asciidoctor: WARNING: <stdin>: line 14: unterminated listing block";
    AsciidocAnnotationResultType asciidocAnnotationResultType = new AsciidocAnnotationResultType(null);
    asciidocAnnotationResultType.addMessage(message, 0);
    AsciidocAnnotationResultType.Message m = asciidocAnnotationResultType.getMessages().get(0);
    assertNotNull(m);
    assertEquals(14, m.getLine().longValue());
    assertEquals(HighlightSeverity.WARNING, m.getSeverity());
  }

  @Test
  public void shouldAddAMessageWithoutLine() {
    String message = "asciidoctor: WARNING: image to embed not found or not readable: ...";
    AsciidocAnnotationResultType asciidocAnnotationResultType = new AsciidocAnnotationResultType(null);
    asciidocAnnotationResultType.addMessage(message, 0);
    AsciidocAnnotationResultType.Message m = asciidocAnnotationResultType.getMessages().get(0);
    assertNotNull(m);
    assertNull(m.getLine());
    assertEquals(HighlightSeverity.WARNING, m.getSeverity());
  }

}

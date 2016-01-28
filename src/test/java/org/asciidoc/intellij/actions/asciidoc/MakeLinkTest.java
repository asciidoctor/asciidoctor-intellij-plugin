package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Michael Krausse, Raffael Krebs, Ulrich Etter
 */
public class MakeLinkTest {

  private MakeLink sut = new MakeLink();

  @Test
  public void testIsLink1() throws Exception {
    assertFalse(sut.isLink("Example"));
  }

  @Test
  public void testIsLink2() throws Exception {
    assertFalse(sut.isLink("www.example.com"));
  }

  @Test
  public void testIsLink3() throws Exception {
    assertTrue(sut.isLink("http://www.example.com"));
  }

  @Test
  public void testIsLink4() throws Exception {
    assertTrue(sut.isLink("http://example.com"));
  }

  @Test
  public void testIsLink5() throws Exception {
    assertTrue(sut.isLink("https://www.example.com"));
  }

  @Test
  public void testIsLink6() throws Exception {
    assertTrue(sut.isLink("https://example.com"));
  }

  @Test
  public void testUpdateSelection1() throws Exception {
    assertEquals("http://example.com[]", sut.updateSelection("http://example.com"));
  }

  @Test
  public void testUpdateSelection2() throws Exception {
    assertEquals("http://www.example.com[www.example.com]", sut.updateSelection("www.example.com"));
  }
}
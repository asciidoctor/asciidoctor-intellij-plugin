package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Schwartz (ahus1)
 */
public class MakeStrikethroughTest {

  private MakeStrikethrough sut = new MakeStrikethrough();

  @Test
  public void shouldAddMarking() throws Exception {
    String actual = sut.updateSelection("about", true);
    assertEquals("[.line-through]#about#", actual);
  }

  @Test
  public void shouldAddTwoMarkings() throws Exception {
    String actual = sut.updateSelection("about", false);
    assertEquals("[.line-through]##about##", actual);
  }

  @Test
  public void shouldRemoveSingleMarking() throws Exception {
    String actual = sut.updateSelection("[.line-through]#about#", false);
    assertEquals("about", actual);
  }

  @Test
  public void shouldRemoveTwoMarkings() throws Exception {
    String actual = sut.updateSelection("[.line-through]##about##", false);
    assertEquals("about", actual);
  }

}
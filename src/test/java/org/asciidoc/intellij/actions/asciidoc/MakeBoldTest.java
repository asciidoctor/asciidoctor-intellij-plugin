package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Krausse (ehmkah)
 */
public class MakeBoldTest {

  private MakeBold sut = new MakeBold();

  @Test
  public void shouldAddAsteriks() throws Exception {
    String actual = sut.updateSelection("about");
    assertEquals("**about**", actual);
  }

  @Test
  public void shouldRemoveSingleAsteriks() throws Exception {
    String actual = sut.updateSelection("*about*");
    assertEquals("about", actual);
  }

  @Test
  public void shouldRemoveTwoAsteriks() throws Exception {
    String actual = sut.updateSelection("**about**");
    assertEquals("about", actual);
  }

  @Test
  public void shouldAddAsteriksBecauseSelectionOnlyStartsWithAsteriks() throws Exception {
    String actual = sut.updateSelection("*about");
    assertEquals("***about**", actual);
  }
}
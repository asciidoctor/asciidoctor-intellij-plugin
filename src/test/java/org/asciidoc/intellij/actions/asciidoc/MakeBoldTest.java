package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Krausse (ehmkah)
 */
public class MakeBoldTest {

  private MakeBold sut = new MakeBold();

  @Test
  public void shouldAddAsterisks() throws Exception {
    String actual = sut.updateSelection("about", false);
    assertEquals("**about**", actual);
  }

  @Test
  public void shouldRemoveSingleAsterisks() throws Exception {
    String actual = sut.updateSelection("*about*", false);
    assertEquals("about", actual);
  }

  @Test
  public void shouldRemoveTwoAsteriks() throws Exception {
    String actual = sut.updateSelection("**about**", false);
    assertEquals("about", actual);
  }

  @Test
  public void shouldAddAsteriksBecauseSelectionOnlyStartsWithAsteriks() throws Exception {
    String actual = sut.updateSelection("*about", false);
    assertEquals("***about**", actual);
  }

  @Test
  public void shouldNotCrashWithTwoAsteriks() throws Exception {
    String actual = sut.updateSelection("**", false);
    assertEquals("", actual);
  }

  @Test
  public void shouldNotCrashWithOneAsteriks() throws Exception {
    String actual = sut.updateSelection("*", false);
    assertEquals("", actual);
  }

  @Test
  public void shouldAddSingleAsteriskWhenIsWord() throws Exception {
    String actual = sut.updateSelection("about", true);
    assertEquals("*about*", actual);
  }

  @Test
  public void shouldRemoveOneWhitespaceWhenUbalanced() throws Exception {
    String actual = sut.updateSelection("**about*", true);
    assertEquals("*about", actual);
  }

}

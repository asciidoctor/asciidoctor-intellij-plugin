package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Michael Krausse (ehmkah)
 * @author Werner Mueller (wemu)
 */
public class MakeTitleTest {

  @Test
  public void testAddTitle() {
    MakeTitle sut = new MakeTitle();
    Assert.assertEquals("= hello", sut.updateSelection("hello"));
  }

  @Test
  public void testAddTitleEmptyLine() {
    MakeTitle sut = new MakeTitle();
    Assert.assertEquals("= ", sut.updateSelection("  "));
  }

  @Test
  public void testAddTitleNewLine() {
    MakeTitle sut = new MakeTitle();
    Assert.assertEquals("= ", sut.updateSelection("\n"));
  }

  @Test
  public void testRemoveTitle() {
    MakeTitle sut = new MakeTitle();
    Assert.assertEquals("hello", sut.updateSelection("= hello"));
  }

}

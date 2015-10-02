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
  public void testRemoveTitle() {
    MakeTitle sut = new MakeTitle();
    Assert.assertEquals("hello", sut.updateSelection("= hello"));
  }

}
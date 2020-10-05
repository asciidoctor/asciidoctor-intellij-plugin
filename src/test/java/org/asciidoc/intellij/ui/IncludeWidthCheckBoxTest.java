package org.asciidoc.intellij.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class IncludeWidthCheckBoxTest {

  private IncludeWidthCheckBox checkBox;

  @Before
  public void setup() {
    checkBox = new IncludeWidthCheckBox();
  }

  @Test
  public void isSetupCorrectly() {
    assertEquals(checkBox.getText(), "Include width");
    assertEquals(checkBox.getToolTipText(), "Enabled once width has been loaded");
    assertFalse(checkBox.isSelected());
    assertFalse(checkBox.isEnabled());
  }

  @Test
  public void invokesSelectedActionOnStateChange() {
    checkBox.onStateChanged(Assert::assertTrue);

    checkBox.setEnabled(true);
  }
}

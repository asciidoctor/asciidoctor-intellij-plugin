package org.asciidoc.intellij.ui.components;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AttributeCheckBoxTest {

  @Test
  public void shouldSetupCorrectly() {
    final String text = "Width:";
    final String toolTip = "Enabled once width has been loaded";

    final AttributeCheckBox checkBox = new AttributeCheckBox(text, toolTip, false);

    assertEquals(checkBox.getText(), text);
    assertEquals(checkBox.getToolTipText(), toolTip);
    assertFalse(checkBox.isSelected());
    assertFalse(checkBox.isEnabled());
  }

  @Test
  public void shouldInvokeSelectedActionOnStateChange() {
    final AttributeCheckBox checkBox = new AttributeCheckBox("");

    checkBox.onStateChanged(Assert::assertTrue);

    checkBox.setEnabled(true);
  }
}

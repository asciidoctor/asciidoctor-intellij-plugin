package org.asciidoc.intellij.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Replacement for SwingX' BoundAction, which is no longer part of the IntelliJ platform as of 2026.3.
 * It carries a label, an action command and an initial selection state for use in a radio button group.
 */
public class OptionAction extends AbstractAction {
  private boolean selected;

  public OptionAction(String name, String command) {
    super(name);
    putValue(ACTION_COMMAND_KEY, command);
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // selection is evaluated by the dialog, nothing to do here
  }
}

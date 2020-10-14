package org.asciidoc.intellij.ui.components;

import com.intellij.ui.JBColor;
import com.intellij.ui.SeparatorComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class GroupSeparator extends JPanel {

  public GroupSeparator(@NotNull final String groupLabel) {
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    add(new JLabel(groupLabel));
    add(Box.createHorizontalStrut(5));
    add(new SeparatorComponent(0, JBColor.GRAY, null));
  }
}

package org.asciidoc.intellij.ui.components;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public final class PasteOptionPanel extends JPanel {

  public PasteOptionPanel(@NotNull final String description, @NotNull final JPanel options) {
    super(new GridLayout(2, 0));

    add(new JLabel(description));
    add(options);
  }
}

package org.asciidoc.intellij.ui;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ItemListener;
import java.util.function.Consumer;

import static java.awt.event.ItemEvent.SELECTED;

public final class IncludeWidthCheckBox extends JCheckBox {

  public IncludeWidthCheckBox() {
    super("Include width", false);
    setEnabled(false);
    setToolTipText("Enabled once width has been loaded");
  }

  public void onStateChanged(@NotNull final Consumer<Boolean> selectedAction) {
    addItemListener(invokeOnStateChange(selectedAction));
  }

  private @NotNull ItemListener invokeOnStateChange(@NotNull final Consumer<Boolean> selectedAction) {
    return event -> selectedAction.accept(event.getStateChange() == SELECTED);
  }
}

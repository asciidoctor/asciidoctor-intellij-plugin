package org.asciidoc.intellij.ui.components;

import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ItemListener;
import java.util.function.Consumer;

import static java.awt.event.ItemEvent.SELECTED;

public final class AttributeCheckBox extends JBCheckBox {

  public AttributeCheckBox(@NotNull final String text) {
    super(text);
  }

  public AttributeCheckBox(@NotNull final String text,
                           @NotNull final String toolTip,
                           final boolean enabled) {
    super(text, false);
    setToolTipText(toolTip);
    setEnabled(enabled);
  }

  public void onStateChanged(@NotNull final Consumer<Boolean> selectedAction) {
    addItemListener(invokeOnStateChange(selectedAction));
  }

  private @NotNull ItemListener invokeOnStateChange(@NotNull final Consumer<Boolean> selectedAction) {
    return event -> selectedAction.accept(event.getStateChange() == SELECTED);
  }
}

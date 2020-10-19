package org.asciidoc.intellij.ui.components;

import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class ImageAttributesPanel extends JPanel {

  private final GroupSeparator groupSeparator = new GroupSeparator("Attributes");
  private final AttributeCheckBox widthCheckBox = new AttributeCheckBox("Width:", "Enabled once width has been loaded", false);
  private final ImageWidthField widthField = new ImageWidthField();
  private final AttributeCheckBox altCheckBox = new AttributeCheckBox("Alt text:");
  private final JBTextField altTextField = new JBTextField();

  public ImageAttributesPanel(@NotNull final CompletableFuture<Optional<Integer>> initialWidthFuture) {
    widthCheckBox.onStateChanged(widthField::setEnabled);
    widthField.initializeWith(initialWidthFuture, this::enableCheckboxAndRemoveTooltip);
    altTextField.setEnabled(false);
    altCheckBox.onStateChanged(altTextField::setEnabled);

    setupPanel();
  }

  private void enableCheckboxAndRemoveTooltip() {
    widthCheckBox.setEnabled(true);
    widthCheckBox.setToolTipText(null);
  }

  private void setupPanel() {
    setLayout(new GridBagLayout());
    final GridBagConstraints c = new GridBagConstraints();

    c.anchor = GridBagConstraints.WEST;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.ipady = 5;

    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 3;
    add(groupSeparator, c);

    c.gridwidth = 1;
    c.gridy = 1;
    c.insets = JBUI.insetsLeft(15);
    add(widthCheckBox, c);

    c.gridx = 1;
    add(widthField, c);

    c.gridx = 2;
    add(Box.createHorizontalStrut(100), c);

    c.gridx = 0;
    c.gridy = 2;
    add(altCheckBox, c);

    c.weightx = 1;
    c.gridx = 1;
    c.gridwidth = 2;
    add(altTextField, c);
  }

  public @NotNull ImageWidthField getWidthField() {
    return widthField;
  }

  public @NotNull JBTextField getAltField() {
    return altTextField;
  }
}

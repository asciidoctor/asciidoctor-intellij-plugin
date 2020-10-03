package org.asciidoc.intellij.ui;

import com.intellij.openapi.util.Pair;
import org.jdesktop.swingx.action.AbstractActionExt;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public final class OptionsPanelFactory {

  public static Pair<JPanel, ButtonGroup> createOptionPanelWithButtonGroup(final List<Action> options) {
    JPanel optionsPanel = new JPanel(new GridLayout(options.size(), 1));
    ButtonGroup buttonGroup = new ButtonGroup();

    boolean selectFirst = true;

    for (Action option : options) {
      if (option instanceof AbstractActionExt && ((AbstractActionExt) option).isSelected()) {
        selectFirst = false;
      }
    }

    for (Action option : options) {
      JRadioButton radioButton = new JRadioButton(option);
      if (selectFirst) {
        radioButton.setSelected(true);
        selectFirst = false;
      } else if (option instanceof AbstractActionExt && ((AbstractActionExt) option).isSelected()) {
        radioButton.setSelected(true);
      }
      buttonGroup.add(radioButton);
      optionsPanel.add(radioButton);
    }

    return new Pair<>(optionsPanel, buttonGroup);
  }
}

package org.asciidoc.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jdesktop.swingx.action.AbstractActionExt;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RadioButtonDialog extends DialogWrapper {

  private String text;
  private List<Action> options;
  private final ButtonGroup buttonGroup = new ButtonGroup();

  public RadioButtonDialog(@Nls(capitalization = Nls.Capitalization.Title) String title, String text, List<Action> options) {
    super(false);
    setTitle(title);
    this.text = text;
    this.options = options;
    setResizable(false);
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridLayout(2, 0));

    JLabel explanation = new JLabel(text);
    panel.add(explanation);

    JPanel optionsPanel = new JPanel(new GridLayout(options.size(), 1));
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
    panel.add(optionsPanel);

    return panel;
  }

  public String getSelectedActionCommand() {
    return buttonGroup.getSelection().getActionCommand();
  }
}

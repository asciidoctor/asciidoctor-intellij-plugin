package org.asciidoc.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RadioButtonDialog extends DialogWrapper {

  private String text;
  private List<Action> options;
  private final ButtonGroup buttonGroup = new ButtonGroup();

  public RadioButtonDialog(String title, String text, List<Action> options) {
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
    boolean first = true;
    for (Action option : options) {
      JRadioButton radioButton = new JRadioButton(option);
      if (first) {
        radioButton.setSelected(true);
        first = false;
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

package org.asciidoc.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.asciidoc.intellij.ui.OptionsPanelFactory.createOptionPanelWithButtonGroup;

public class RadioButtonDialog extends DialogWrapper {

  private final String text;
  private final JPanel optionsPanel;
  private final ButtonGroup buttonGroup;

  public RadioButtonDialog(@Nls(capitalization = Nls.Capitalization.Title) String title, String text, List<Action> options) {
    super(false);
    setTitle(title);
    this.text = text;

    Pair<JPanel, ButtonGroup> optionPanelWithButtonGroup = createOptionPanelWithButtonGroup(options);
    optionsPanel = optionPanelWithButtonGroup.getFirst();
    buttonGroup = optionPanelWithButtonGroup.getSecond();
    setResizable(false);
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridLayout(2, 0));

    panel.add(new JLabel(text));
    panel.add(optionsPanel);

    return panel;
  }

  public String getSelectedActionCommand() {
    return buttonGroup.getSelection().getActionCommand();
  }
}

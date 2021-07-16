package org.asciidoc.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class OverwriteFileDialog extends DialogWrapper {

  private final String filename;

  public OverwriteFileDialog(String filename) {
    super(true);
    setTitle("File Already Exists");
    this.filename = filename;
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel dialogPanel = new JPanel(new BorderLayout());

    JLabel label = new JLabel("File \"" + filename + "\" already exists. Do you want to overwrite this file?");
    label.setPreferredSize(new Dimension(100, 50));
    dialogPanel.add(label, BorderLayout.CENTER);

    return dialogPanel;
  }
}

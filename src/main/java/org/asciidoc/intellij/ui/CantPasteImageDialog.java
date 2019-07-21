package org.asciidoc.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CantPasteImageDialog extends DialogWrapper {

  private String text;

  public CantPasteImageDialog() {
    super(false);
    setTitle("Import Image from Clipboard");
    setResizable(false);
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridLayout(2, 0));

    JLabel explanation = new JLabel("Clipboard doesn't contain an image.");
    panel.add(explanation);

    JLabel hint = new JLabel("Please copy an image to the clipboard before using this action");
    panel.add(hint);

    return panel;
  }

}

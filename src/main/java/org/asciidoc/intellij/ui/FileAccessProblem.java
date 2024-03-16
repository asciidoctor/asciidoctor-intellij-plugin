package org.asciidoc.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class FileAccessProblem extends DialogWrapper {

  private final String message;

  @Override
  protected Action @NotNull [] createActions() {
    return new Action[]{getOKAction()};
  }

  @Override
  public void show() {
    super.show();
  }

  public FileAccessProblem(String message) {
    super(true);
    setTitle("File access problem");
    this.message = message;
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel dialogPanel = new JPanel(new BorderLayout());

    JLabel label = new JLabel("<html>" + StringEscapeUtils.escapeHtml4(message) + "</html>");
    label.setPreferredSize(new Dimension(150, 50));
    dialogPanel.add(label, BorderLayout.CENTER);

    return dialogPanel;
  }
}

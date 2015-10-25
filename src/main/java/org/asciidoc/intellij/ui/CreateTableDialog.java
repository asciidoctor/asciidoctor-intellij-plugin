package org.asciidoc.intellij.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CreateTableDialog extends DialogWrapper {
  private SpinnerNumberModel columnCount = new SpinnerNumberModel(3, 1, 99, 1);
  private SpinnerNumberModel rowCount = new SpinnerNumberModel(3, 1, 99, 1);

  private JTextField title = new JTextField("", 5);

  public CreateTableDialog() {
    super(false);
    setTitle("Create Table");
    setResizable(false);
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridLayout(3, 0));

    JPanel addTitle = new JPanel(new BorderLayout());
    addTitle.add(new JLabel("Title"), BorderLayout.LINE_START);
    addTitle.add(new JPanel(), BorderLayout.CENTER); //Spacing between label and text field
    addTitle.add(title, BorderLayout.LINE_END);
    panel.add(addTitle);

    JPanel columns = new JPanel(new BorderLayout());
    columns.add(new JLabel("No of rows"), BorderLayout.CENTER);
    JSpinner rows = new JSpinner(this.rowCount);
    columns.add(rows, BorderLayout.LINE_END);
    panel.add(columns);

    JPanel rowPane = new JPanel(new BorderLayout());
    rowPane.add(new JLabel("No of colums"), BorderLayout.CENTER);
    JSpinner cols = new JSpinner(this.columnCount);
    rowPane.add(cols, BorderLayout.LINE_END);
    panel.add(rowPane);

    return panel;
  }

  public int getRowCount() {
    return rowCount.getNumber().intValue();
  }

  public int getColumnCount() {
    return columnCount.getNumber().intValue();
  }

  public String getTitle() {
    return title.getText();
  }
}

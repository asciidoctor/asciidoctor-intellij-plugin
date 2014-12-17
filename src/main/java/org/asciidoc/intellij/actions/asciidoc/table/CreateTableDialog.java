package org.asciidoc.intellij.actions.asciidoc.table;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CreateTableDialog extends DialogWrapper {
  private SpinnerNumberModel smCols = new SpinnerNumberModel(3 ,1, 99, 1);
  private SpinnerNumberModel smRows = new SpinnerNumberModel(3 ,1, 99, 1);

  private JSpinner cols, rows;
  private JCheckBox title;


  public CreateTableDialog() {
    super(false);
    setTitle("Create table");
    setResizable(false);
    init();
  }


  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridLayout(3, 0));

    JPanel useTitle = new JPanel(new BorderLayout());
    title = new JCheckBox("Use title", true);
    useTitle.add(title, BorderLayout.CENTER);
    panel.add(useTitle);

    JPanel columns = new JPanel(new BorderLayout());
    columns.add(new JLabel("Nr. of rows"), BorderLayout.CENTER);
    rows = new JSpinner(smRows);
    columns.add(rows, BorderLayout.LINE_END);
    panel.add(columns);

    JPanel rowPane = new JPanel(new BorderLayout());
    rowPane.add(new JLabel("Nr. of colums"), BorderLayout.CENTER);
    cols = new JSpinner(smCols);
    rowPane.add(cols, BorderLayout.LINE_END);
    panel.add(rowPane);

    return panel;
  }

  public int getNrofRows() {
    return smRows.getNumber().intValue();
  }

  public int getNrOfColumns() {
    return smCols.getNumber().intValue();
  }

  public boolean useTitle() {
    return title.isSelected();
  }
}

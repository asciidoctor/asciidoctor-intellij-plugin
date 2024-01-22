package org.asciidoc.intellij.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;

public class PasteTableDialog extends DialogWrapper {

  private com.intellij.openapi.diagnostic.Logger log =
    com.intellij.openapi.diagnostic.Logger.getInstance(PasteTableDialog.class);

  private ComboBox separatorSelector = new ComboBox<>(new String[]{"tab", ",", ";", "space"});
  @Nullable
  private String data;
  private String separator;
  private JLabel expectedTableSizeLabel = new JLabel();
  private JCheckBox firstLineHeaderCheckbox = new JCheckBox();

  public PasteTableDialog() {
    super(false);
    setTitle("Paste Table");
    setResizable(false);
    separatorSelector.setEditable(true);
    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridLayout(4, 0));

    JLabel explanation = new JLabel("<html><body>Use this dialog to parse data from your clipboard<br>and paste it as a table in the document.<body><html>");
    panel.add(explanation);

    JPanel firstLineHeaderPanel = new JPanel(new BorderLayout());
    firstLineHeaderPanel.add(new JLabel("First line in the clipboard are headers:"), BorderLayout.LINE_START);
    firstLineHeaderPanel.add(new JPanel(), BorderLayout.CENTER);
    firstLineHeaderPanel.add(firstLineHeaderCheckbox, BorderLayout.LINE_END);
    panel.add(firstLineHeaderPanel);

    JPanel pasteTableSeparator = new JPanel(new BorderLayout());
    pasteTableSeparator.add(new JLabel("Separator:"), BorderLayout.LINE_START);
    pasteTableSeparator.add(new JPanel(), BorderLayout.CENTER);
    pasteTableSeparator.add(separatorSelector, BorderLayout.LINE_END);
    panel.add(pasteTableSeparator);

    JPanel result = new JPanel(new BorderLayout());
    result.add(new JLabel("Expected Table size:"), BorderLayout.LINE_START);
    result.add(new JPanel(), BorderLayout.CENTER);
    result.add(expectedTableSizeLabel, BorderLayout.LINE_END);
    panel.add(result);

    return panel;
  }

  @Override
  protected boolean postponeValidation() {
    return false;
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    // reset data first in case the result can't be parsed or no separator given
    data = null;
    String sep = (String) separatorSelector.getSelectedItem();
    if (sep == null) {
      sep = "";
    }
    if (sep.equals("tab")) {
      separator = "\t";
    } else if (sep.equals("space")) {
      separator = " ";
    } else {
      separator = sep;
    }
    if (separator.length() > 0) {
      try {
        data = (String) Toolkit.getDefaultToolkit()
          .getSystemClipboard().getData(DataFlavor.stringFlavor);
        BufferedReader br = new BufferedReader(new CharArrayReader(data.toCharArray()));
        long lines = br.lines().count();
        br = new BufferedReader(new CharArrayReader(data.toCharArray()));
        int max = br.lines().mapToInt(line -> StringUtils.countMatches(line, separator)).max().orElse(0);
        expectedTableSizeLabel.setText(max + 1 + " x " + lines);
      } catch (UnsupportedFlavorException | IOException e) {
        log.info("unable to parse clipboard (no text found)", e);
        expectedTableSizeLabel.setText("");
        return new ValidationInfo("Unable to parse clipboard (no text found)");
      }
    } else {
      expectedTableSizeLabel.setText("");
      return new ValidationInfo("Please select separator", separatorSelector);
    }
    return super.doValidate();
  }

  @Nullable
  public String getData() {
    return data;
  }

  public String getSeparator() {
    return separator;
  }

  public boolean isFirstLineHeader() {
    return firstLineHeaderCheckbox.isSelected();
  }
}

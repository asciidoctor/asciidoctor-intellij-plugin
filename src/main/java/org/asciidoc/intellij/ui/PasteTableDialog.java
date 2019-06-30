package org.asciidoc.intellij.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;

public class PasteTableDialog extends DialogWrapper {

  private ComboBox separatorSelector = new ComboBox<String>(new String[]{"tab", ",", ";", "space"});
  private String data;
  private String separator;
  private JLabel expectedTableSizeLabel = new JLabel();

  public PasteTableDialog() {
    super(false);
    setTitle("Paste Table");
    setResizable(false);
    separatorSelector.setEditable(true);

    separatorSelector.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        update();
      }
    });

    update();

    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridLayout(3, 0));

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


    update();

    return panel;
  }

  private void update() {
    String sep = (String) separatorSelector.getSelectedItem();
    if (sep.equals("tab")) {
      separator = "\t";
    } else if (sep.equals("space")) {
      separator = " ";
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
      } catch (UnsupportedFlavorException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
        expectedTableSizeLabel.setText("");
      }
    } else {
      expectedTableSizeLabel.setText("");
    }
  }

  public String getData() {
    return data;
  }

  public String getSeparator() {
    return separator;
  }
}

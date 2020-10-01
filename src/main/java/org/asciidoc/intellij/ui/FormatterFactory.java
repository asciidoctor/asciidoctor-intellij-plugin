package org.asciidoc.intellij.ui;

import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;

import static java.lang.Integer.MAX_VALUE;

public final class FormatterFactory {

  public static NumberFormatter createIntegerFormatter() {
    NumberFormatter formatter = new NumberFormatter(NumberFormat.getInstance());

    formatter.setValueClass(Integer.class);
    formatter.setMinimum(0);
    formatter.setMaximum(MAX_VALUE);
    formatter.setAllowsInvalid(false);
    formatter.setCommitsOnValidEdit(true);

    return formatter;
  }
}

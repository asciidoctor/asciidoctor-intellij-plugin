package org.asciidoc.intellij.ui;

import org.junit.Test;

import javax.swing.text.NumberFormatter;

import static java.lang.Integer.MAX_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

public class FormatterFactoryTest {

  @Test
  public void createIntegerFormatter() {
    NumberFormatter formatter = FormatterFactory.createIntegerFormatter();

    assertThat(formatter.getValueClass()).isEqualTo(Integer.class);
    assertThat(formatter.getMinimum().equals(0)).isTrue();
    assertThat(formatter.getMaximum().equals(MAX_VALUE)).isTrue();
    assertThat(formatter.getAllowsInvalid()).isFalse();
    assertThat(formatter.getCommitsOnValidEdit()).isTrue();
  }
}

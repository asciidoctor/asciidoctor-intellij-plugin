package org.asciidoc.intellij.asciidoc;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.util.Arrays;

/**
 * Prepend a configuration before a file.
 * This will push back the configuration lines at the beginning.
 * This will not change the line original numbers in the file.
 * If messages are reported for the configuration lines, they will receive negative numbers.
 */
public class PrependConfig extends Preprocessor {
  private String config = "";

  @Override
  public void process(Document document, PreprocessorReader reader) {
    if (config.length() != 0) {
      // otherwise an empty line at the beginning breaks level 0 detection
      reader.restoreLines(Arrays.asList(config.split("\n")));
    }
  }

  public void setConfig(String config) {
    this.config = config;
  }
}

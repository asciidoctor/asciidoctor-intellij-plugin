package org.asciidoc.intellij.asciidoc;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.util.Collections;

/**
 * Prepend a configuration before a file.
 * This will push back the configuration lines at the beginning.
 * This will not change the line original numbers in the file.
 * If processing reports messages for the configuration lines, they will receive line numbers starting with 1.
 * Once the processing is complete, all lines from the regular document receive regular line numbers.
 */
public class PrependConfig extends Preprocessor {
  private String config = "";

  @Override
  public void process(Document document, PreprocessorReader reader) {
    if (config.length() != 0) {
      // otherwise an empty line at the beginning breaks level 0 detection
      reader.push_include(config, null, null, 1, Collections.emptyMap());
    }
  }

  public void setConfig(String config) {
    this.config = config;
  }
}

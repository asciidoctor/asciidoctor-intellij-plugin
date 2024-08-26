package org.asciidoc.intellij.asciidoc;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.asciidoctor.extension.Reader;

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
  public Reader process(Document document, PreprocessorReader reader) {
    if (!config.isEmpty()) {
      // otherwise, an empty line at the beginning breaks level 0 detection
      reader.pushInclude(config, null, null, 1, Collections.emptyMap());
    }
    return null;
  }

  public void setConfig(String config) {
    this.config = config;
  }
}

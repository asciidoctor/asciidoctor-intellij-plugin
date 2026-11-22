package org.asciidoc.intellij.asciidoc;

import org.asciidoc.intellij.lexer.AsciiDocFreemarker;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.asciidoctor.extension.Reader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Neutralizes a FreeMarker template when a file is converted, for example for an export.
 * For the preview this is done on the text before it is passed to Asciidoctor, therefore this is only active when a file is set.
 * The number of lines doesn't change.
 */
public class FreemarkerPreprocessor extends Preprocessor {
  private volatile File file;
  private volatile File boundary;

  @Override
  public Reader process(Document document, PreprocessorReader reader) {
    File currentFile = file;
    if (currentFile == null) {
      return null;
    }
    List<String> lines = reader.readLines();
    String text = String.join("\n", lines);
    String neutralized = AsciiDocFreemarker.neutralize(text, currentFile, boundary);
    if (neutralized.equals(text)) {
      reader.restoreLines(lines);
    } else {
      // pushed lines are processed again, so that the includes that were added are resolved
      reader.pushInclude(neutralized, currentFile.getPath(), currentFile.getName(), 1, Collections.emptyMap());
    }
    return null;
  }

  public void setFile(@Nullable File file, @Nullable File boundary) {
    this.file = file;
    this.boundary = boundary;
  }
}

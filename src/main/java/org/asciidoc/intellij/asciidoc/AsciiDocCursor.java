package org.asciidoc.intellij.asciidoc;

import org.asciidoctor.ast.Cursor;

/**
 * Implementation of a Cursor; to be used for Asciidoctor Java extensions.
 */
public class AsciiDocCursor implements Cursor {
  private final int lineNumber;
  private final String path;
  private final String dir;
  private final String file;

  public AsciiDocCursor(String file, String path, String dir, int lineNumber) {
    this.lineNumber = lineNumber;
    this.path = path;
    this.dir = dir;
    this.file = file;
  }

  @Override
  public int getLineNumber() {
    return lineNumber;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getDir() {
    return dir;
  }

  @Override
  public String getFile() {
    return file;
  }
}

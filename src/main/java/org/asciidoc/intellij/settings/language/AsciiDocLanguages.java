package org.asciidoc.intellij.settings.language;

import lombok.Getter;

import java.io.Serializable;

@Getter
public enum AsciiDocLanguages implements Serializable {
  GO(Indices.INDEX_GO),
  JAVA_SCRIPT(Indices.INDEX_JAVA_SCRIPT),
  POWER_SHELL(Indices.INDEX_POWER_SHELL),
  PYTHON(Indices.INDEX_PYTHON),
  RUBY(Indices.INDEX_RUBY),
  TYPE_SCRIPT(Indices.INDEX_TYPE_SCRIPT);

  private final int index;

  AsciiDocLanguages(int index) {
    this.index = index;
  }

  public record Indices() {
    static final int INDEX_GO = 0;
    static final int INDEX_JAVA_SCRIPT = 1;
    static final int INDEX_POWER_SHELL = 2;
    static final int INDEX_PYTHON = 3;
    static final int INDEX_RUBY = 4;
    static final int INDEX_TYPE_SCRIPT = 5;
  }
}

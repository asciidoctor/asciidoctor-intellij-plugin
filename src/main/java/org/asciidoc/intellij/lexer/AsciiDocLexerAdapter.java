package org.asciidoc.intellij.lexer;

import com.intellij.lexer.FlexAdapter;

/**
 * @author Alexander Schwartz
 */
public class AsciiDocLexerAdapter extends FlexAdapter {
  public AsciiDocLexerAdapter() {
    super(new _AsciiDocLexer(null));
  }
}

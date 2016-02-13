package org.asciidoc.intellij.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.MergingLexerAdapter;

/**
 * @author yole
 */
public class AsciiDocLexer extends MergingLexerAdapter {
  public AsciiDocLexer() {
    super(new FlexAdapter(new _AsciiDocLexer(null)), AsciiDocTokenTypes.TOKENS_TO_MERGE);
  }
}

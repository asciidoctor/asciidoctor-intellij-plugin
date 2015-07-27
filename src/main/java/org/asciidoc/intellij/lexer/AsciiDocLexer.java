package org.asciidoc.intellij.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.TokenSet;

/**
 * @author yole
 */
public class AsciiDocLexer extends MergingLexerAdapter {
  public AsciiDocLexer() {
    super(new FlexAdapter(new _AsciiDocLexer(null)),
        TokenSet.create(AsciiDocTokenTypes.TEXT, AsciiDocTokenTypes.LISTING_TEXT, AsciiDocTokenTypes.HEADING,
            AsciiDocTokenTypes.BLOCK_COMMENT));
  }
}

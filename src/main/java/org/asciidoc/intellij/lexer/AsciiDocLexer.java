package org.asciidoc.intellij.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.IElementType;

import java.io.IOException;

/**
 * @author yole
 */
public class AsciiDocLexer extends MergingLexerAdapter {
  public AsciiDocLexer() {
    super(new FlexAdapter(new _AsciiDocLexer(null) {
      @Override
      public void reset(CharSequence buffer, int start, int end, int initialState) {
        super.reset(buffer, start, end, initialState);
        setFinal(end);
      }

      @Override
      public IElementType advance() throws IOException {
        limitLookahead();
        IElementType advance = super.advance();
        clearLookahead();
        return advance;
      }
    }), AsciiDocTokenTypes.TOKENS_TO_MERGE);
  }
}

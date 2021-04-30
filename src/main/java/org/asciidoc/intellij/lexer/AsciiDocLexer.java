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
      // limit of full pushbacks per parsed token
      private static final int MAX_PUSHBACKS = 10000;
      private int pushbacks = 0;
      @Override
      public void reset(CharSequence buffer, int start, int end, int initialState) {
        super.reset(buffer, start, end, initialState);
        setFinal(end);
      }

      /**
       * This overrides yypushback to count pushbacks in order to detect infinite loops.
       * Infinite loops might otherwise freeze the UI and make the IDE unusable.
       */
      @Override
      public void yypushback(int number) {
        if (number > 0 && number == this.yylength()) {
          // full length of the token is being pushed back
          pushbacks++;
          if (pushbacks > MAX_PUSHBACKS) {
            throw new IllegalStateException("Too many pushbacks, suspecting infinite loop in string '"
              + this.getBuffer().toString() + "' at position " + this.getTokenStart());
          }
        }
        super.yypushback(number);
      }

      private void resetCounterForPushbacks() {
        pushbacks = 0;
      }

      @Override
      public IElementType advance() throws IOException {
        limitLookahead();
        resetCounterForPushbacks();
        IElementType advance = super.advance();
        clearLookahead();
        return advance;
      }

    }), AsciiDocTokenTypes.TOKENS_TO_MERGE);
  }
}

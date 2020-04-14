package org.asciidoc.intellij.indexer;

import com.intellij.lexer.Lexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.cache.impl.BaseFilterLexer;
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;

/**
 * @author Michael Krausse (ehmkah)
 */
public class AsciiDocFilterLexer extends BaseFilterLexer {

  private static final TokenSet OUR_SKIP_WORDS_SCAN_SET = TokenSet.create(
    TokenType.WHITE_SPACE,
    AsciiDocTokenTypes.LPAREN,
    AsciiDocTokenTypes.LINE_BREAK,
    AsciiDocTokenTypes.BLOCKIDSTART,
    AsciiDocTokenTypes.BLOCKIDEND,
    AsciiDocTokenTypes.RPAREN,
    AsciiDocTokenTypes.LBRACKET,
    AsciiDocTokenTypes.RBRACKET,
    AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER,
    AsciiDocTokenTypes.BLOCK_DELIMITER,
    AsciiDocTokenTypes.EMPTY_LINE,
    AsciiDocTokenTypes.ATTRIBUTE_REF_START,
    AsciiDocTokenTypes.ATTRIBUTE_REF_END,
    AsciiDocTokenTypes.ATTRIBUTE_NAME_START,
    AsciiDocTokenTypes.ATTRIBUTE_NAME_END,
    AsciiDocTokenTypes.SEPARATOR,
    AsciiDocTokenTypes.DOUBLE_QUOTE,
    AsciiDocTokenTypes.SINGLE_QUOTE,
    AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER,
    AsciiDocTokenTypes.ITALIC_START,
    AsciiDocTokenTypes.ITALIC_END,
    AsciiDocTokenTypes.MONO_START,
    AsciiDocTokenTypes.MONO_END,
    AsciiDocTokenTypes.BOLD_START,
    AsciiDocTokenTypes.BOLD_END,
    AsciiDocTokenTypes.ATTRS_START,
    AsciiDocTokenTypes.ATTRS_END,
    AsciiDocTokenTypes.HARD_BREAK,
    AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_START,
    AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_END,
    AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START,
    AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_END,
    AsciiDocTokenTypes.URL_START,
    AsciiDocTokenTypes.LINKSTART,
    AsciiDocTokenTypes.BLOCK_MACRO_ID,
    AsciiDocTokenTypes.INLINE_MACRO_ID,
    AsciiDocTokenTypes.URL_END,
    AsciiDocTokenTypes.FRONTMATTER_DELIMITER,
    AsciiDocTokenTypes.BIBSTART,
    AsciiDocTokenTypes.BIBEND
  );

  private static final TokenSet JAVA_TOKENS = TokenSet.create(
    AsciiDocTokenTypes.MONO,
    AsciiDocTokenTypes.ITALIC,
    AsciiDocTokenTypes.MONOBOLD,
    AsciiDocTokenTypes.MONOBOLDITALIC,
    AsciiDocTokenTypes.BOLDITALIC
  );

  public AsciiDocFilterLexer(Lexer lexer, OccurrenceConsumer consumer) {
    super(lexer, consumer);
  }

  @Override
  public void advance() {
    final IElementType tokenType = myDelegate.getTokenType();

    if (tokenType == AsciiDocTokenTypes.TEXT) {
      scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT, false, false);
    } else if (tokenType == AsciiDocTokenTypes.BLOCKID) {
      addOccurrenceInToken(UsageSearchContext.IN_CODE);
    } else if (tokenType == AsciiDocTokenTypes.LINKANCHOR) {
      addOccurrenceInToken(UsageSearchContext.IN_CODE);
    } else if (JAVA_TOKENS.contains(tokenType)) {
      scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT | UsageSearchContext.IN_CODE, false, false);
    } else if (tokenType == AsciiDocTokenTypes.LISTING_TEXT || tokenType == AsciiDocTokenTypes.PASSTRHOUGH_CONTENT) {
      // listings can contain other languages
      scanWordsInToken(UsageSearchContext.IN_STRINGS | UsageSearchContext.IN_FOREIGN_LANGUAGES, false, true);
    } else if (tokenType == AsciiDocTokenTypes.BLOCK_COMMENT || tokenType == AsciiDocTokenTypes.LINE_COMMENT) {
      // comments can contain TODOs, but also tags for includes (therefore add code here)
      scanWordsInToken(UsageSearchContext.IN_COMMENTS | UsageSearchContext.IN_CODE, false, false);
      advanceTodoItemCountsInToken();
    } else if (!OUR_SKIP_WORDS_SCAN_SET.contains(tokenType)) {
      // text can contain references to JavaClasses, therefore use code and plain text here
      scanWordsInToken(UsageSearchContext.IN_PLAIN_TEXT | UsageSearchContext.IN_CODE, false, false);
    }
    myDelegate.advance();
  }

}

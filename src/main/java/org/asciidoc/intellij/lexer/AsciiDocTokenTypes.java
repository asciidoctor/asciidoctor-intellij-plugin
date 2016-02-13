package org.asciidoc.intellij.lexer;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author yole
 */
public interface AsciiDocTokenTypes {
  IElementType TEXT = new AsciiDocElementType("TEXT");
  IElementType LINE_BREAK = new AsciiDocElementType("LINE_BREAK");
  IElementType LINE_COMMENT = new AsciiDocElementType("LINE_COMMENT");
  IElementType BLOCK_COMMENT = new AsciiDocElementType("BLOCK_COMMENT");
  IElementType LISTING_DELIMITER = new AsciiDocElementType("LISTING_DELIMITER");
  IElementType LISTING_TEXT = new AsciiDocElementType("LISTING_TEXT");
  IElementType HEADING = new AsciiDocElementType("HEADING");
  IElementType TITLE = new AsciiDocElementType("TITLE");
  IElementType BLOCK_MACRO_ID = new AsciiDocElementType("BLOCK_MACRO_ID");
  IElementType BLOCK_MACRO_BODY = new AsciiDocElementType("BLOCK_MACRO_BODY");
  IElementType BLOCK_MACRO_ATTRIBUTES = new AsciiDocElementType("BLOCK_MACRO_ATTRIBUTES");
  IElementType EXAMPLE_BLOCK_DELIMITER = new AsciiDocElementType("EXAMPLE_BLOCK_DELIMITER");

  TokenSet TOKENS_TO_MERGE = TokenSet.create(TEXT, LISTING_TEXT, HEADING, TITLE, BLOCK_COMMENT,
      BLOCK_MACRO_BODY, BLOCK_MACRO_ATTRIBUTES);
}


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
  IElementType SIDEBAR_BLOCK_DELIMITER = new AsciiDocElementType("SIDEBAR_BLOCK_DELIMITER");
  IElementType BLOCK_ATTRS_START = new AsciiDocElementType("BLOCK_ATTRS_START");
  IElementType BLOCK_ATTR_NAME = new AsciiDocElementType("BLOCK_ATTR_NAME");
  IElementType BLOCK_ATTRS_END = new AsciiDocElementType("BLOCK_ATTRS_END");

  TokenSet TOKENS_TO_MERGE = TokenSet.create(TEXT, LISTING_TEXT, HEADING, TITLE, BLOCK_COMMENT,
      BLOCK_ATTR_NAME, BLOCK_MACRO_BODY, BLOCK_MACRO_ATTRIBUTES);
}


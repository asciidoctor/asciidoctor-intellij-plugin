package org.asciidoc.intellij.lexer;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author yole
 */
public interface AsciiDocTokenTypes {
  IElementType TEXT = new AsciiDocElementType("TEXT");
  IElementType BULLET = new AsciiDocElementType("BULLET");
  IElementType LINE_BREAK = new AsciiDocElementType("LINE_BREAK");
  IElementType WHITE_SPACE = new AsciiDocElementType("WHITE_SPACE");
  IElementType LINE_COMMENT = new AsciiDocElementType("LINE_COMMENT");
  IElementType BLOCK_COMMENT = new AsciiDocElementType("BLOCK_COMMENT");
  IElementType PASSTRHOUGH_BLOCK_DELIMITER = new AsciiDocElementType("PASSTRHOUGH_BLOCK_DELIMITER");
  IElementType PASSTRHOUGH_INLINE_START = new AsciiDocElementType("PASSTRHOUGH_INLINE_START");
  IElementType PASSTRHOUGH_INLINE_END = new AsciiDocElementType("PASSTRHOUGH_INLINE_END");
  IElementType PASSTRHOUGH_CONTENT = new AsciiDocElementType("PASSTRHOUGH_CONTENT");
  IElementType BLOCK_DELIMITER = new AsciiDocElementType("BLOCK_DELIMITER");
  IElementType LITERAL_BLOCK_DELIMITER = new AsciiDocElementType("LITERAL_BLOCK_DELIMITER");
  IElementType LITERAL_BLOCK = new AsciiDocElementType("LITERAL_BLOCK");
  IElementType LISTING_BLOCK_DELIMITER = new AsciiDocElementType("LISTING_BLOCK_DELIMITER");
  IElementType COMMENT_BLOCK_DELIMITER = new AsciiDocElementType("COMMENT_BLOCK_DELIMITER");
  IElementType LISTING_TEXT = new AsciiDocElementType("LISTING_TEXT");
  IElementType CODE_FENCE_CONTENT = new AsciiDocElementType("CODE_FENCE_CONTENT");
  IElementType HEADING = new AsciiDocElementType("HEADING");
  IElementType HEADING_OLDSTYLE = new AsciiDocElementType("HEADING_OLDSTYLE");
  IElementType TITLE = new AsciiDocElementType("TITLE");
  IElementType BLOCK_MACRO_ID = new AsciiDocElementType("BLOCK_MACRO_ID");
  IElementType BLOCK_MACRO_BODY = new AsciiDocElementType("BLOCK_MACRO_BODY");
  IElementType BLOCK_MACRO_ATTRIBUTES = new AsciiDocElementType("BLOCK_MACRO_ATTRIBUTES");
  IElementType BLOCK_ATTRS_START = new AsciiDocElementType("BLOCK_ATTRS_START");
  IElementType BLOCK_ATTR_NAME = new AsciiDocElementType("BLOCK_ATTR_NAME");
  IElementType BLOCK_ATTR_VALUE = new AsciiDocElementType("BLOCK_ATTR_VALUE");
  IElementType BLOCK_ATTRS_END = new AsciiDocElementType("BLOCK_ATTRS_END");
  IElementType BOLD_START = new AsciiDocElementType("BOLD_START");
  IElementType BOLD_END = new AsciiDocElementType("BOLD_END");
  IElementType BOLD = new AsciiDocElementType("BOLD");
  IElementType ITALIC_START = new AsciiDocElementType("ITALIC_START");
  IElementType ITALIC_END = new AsciiDocElementType("ITALIC_END");
  IElementType MONO_START = new AsciiDocElementType("MONO_START");
  IElementType MONO_END = new AsciiDocElementType("MONO_END");
  IElementType ITALIC = new AsciiDocElementType("ITALIC");
  IElementType BOLDITALIC = new AsciiDocElementType("BOLDITALIC");
  IElementType MONO = new AsciiDocElementType("MONO");
  IElementType LPAREN = new AsciiDocElementType("LPAREN");
  IElementType RPAREN = new AsciiDocElementType("RPAREN");
  IElementType LBRACKET = new AsciiDocElementType("LBRACKET");
  IElementType RBRACKET = new AsciiDocElementType("RBRACKET");
  IElementType MONOBOLD = new AsciiDocElementType("MONOBOLD");
  IElementType MONOITALIC = new AsciiDocElementType("MONOITALIC");
  IElementType MONOBOLDITALIC = new AsciiDocElementType("MONOBOLDITALIC");
  IElementType LT = new AsciiDocElementType("LT");
  IElementType GT = new AsciiDocElementType("GT");
  IElementType DOUBLE_QUOTE = new AsciiDocElementType("DOUBLE_QUOTE");
  IElementType SINGLE_QUOTE = new AsciiDocElementType("SINGLE_QUOTE");
  IElementType REFSTART = new AsciiDocElementType("REFSTART");
  IElementType REF = new AsciiDocElementType("REF");
  IElementType REFFILE = new AsciiDocElementType("REFFILE");
  IElementType REFTEXT = new AsciiDocElementType("REFTEXT");
  IElementType REFEND = new AsciiDocElementType("REFEND");
  IElementType BLOCKIDSTART = new AsciiDocElementType("BLOCKIDSTART");
  IElementType BLOCKID = new AsciiDocElementType("BLOCKID");
  IElementType BLOCKREFTEXT = new AsciiDocElementType("BLOCKREFTEXT");
  IElementType BLOCKIDEND = new AsciiDocElementType("BLOCKIDEND");
  IElementType SEPARATOR = new AsciiDocElementType("SEPARATOR");
  IElementType TYPOGRAPHIC_DOUBLE_QUOTE_START = new AsciiDocElementType("TYPOGRAPHIC_DOUBLE_QUOTE_START");
  IElementType TYPOGRAPHIC_DOUBLE_QUOTE_END = new AsciiDocElementType("TYPOGRAPHIC_DOUBLE_QUOTE_END");
  IElementType TYPOGRAPHIC_SINGLE_QUOTE_START = new AsciiDocElementType("TYPOGRAPHIC_SINGLE_QUOTE_START");
  IElementType TYPOGRAPHIC_SINGLE_QUOTE_END = new AsciiDocElementType("TYPOGRAPHIC_SINGLE_QUOTE_END");
  IElementType LINK = new AsciiDocElementType("LINK");
  IElementType LINKSTART = new AsciiDocElementType("LINKSTART");
  IElementType LINKFILE = new AsciiDocElementType("LINKFILE");
  IElementType LINKANCHOR = new AsciiDocElementType("LINKANCHOR");
  IElementType LINKTEXT_START = new AsciiDocElementType("LINKTEXT_START");
  IElementType LINKTEXT = new AsciiDocElementType("LINKTEXT");
  IElementType LINKEND = new AsciiDocElementType("LINKEND");
  IElementType ATTRIBUTE_NAME_START = new AsciiDocElementType("ATTRIBUTE_NAME_START");
  IElementType ATTRIBUTE_NAME = new AsciiDocElementType("ATTRIBUTE_NAME");
  IElementType ATTRIBUTE_NAME_END = new AsciiDocElementType("ATTRIBUTE_NAME_END");
  IElementType ATTRIBUTE_VAL = new AsciiDocElementType("ATTRIBUTE_VAL");
  IElementType ATTRIBUTE_REF_START = new AsciiDocElementType("ATTRIBUTE_REF_START");
  IElementType ATTRIBUTE_REF = new AsciiDocElementType("ATTRIBUTE_REF");
  IElementType ATTRIBUTE_REF_END = new AsciiDocElementType("ATTRIBUTE_REF_END");
  IElementType PAGEBREAK = new AsciiDocElementType("PAGEBREAK");
  IElementType HORIZONTALRULE = new AsciiDocElementType("HORIZONTALRULE");

  TokenSet TOKENS_TO_MERGE = TokenSet.create(TEXT, LISTING_TEXT, HEADING, TITLE, BLOCK_COMMENT,
      BLOCK_ATTR_NAME, BLOCK_MACRO_BODY, BLOCK_MACRO_ATTRIBUTES, PASSTRHOUGH_CONTENT,
      BOLD_START, BOLD_END, BOLD, ITALIC, ITALIC_END, ITALIC_START, BOLDITALIC,
      MONO_START, MONO_END, MONO, MONOBOLD, MONOBOLDITALIC, REF, BLOCKID, BLOCKREFTEXT, REFTEXT, WHITE_SPACE, LINKTEXT,
      LINKFILE, LINKANCHOR, ATTRIBUTE_NAME, ATTRIBUTE_VAL, ATTRIBUTE_REF, CODE_FENCE_CONTENT, LITERAL_BLOCK);
}


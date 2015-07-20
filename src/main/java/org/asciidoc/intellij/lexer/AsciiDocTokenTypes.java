package org.asciidoc.intellij.lexer;

import com.intellij.psi.tree.IElementType;

/**
 * @author yole
 */
public interface AsciiDocTokenTypes {
  IElementType TEXT = new AsciiDocElementType("TEXT");
  IElementType LINE_BREAK = new AsciiDocElementType("LINE_BREAK");
  IElementType LINE_COMMENT = new AsciiDocElementType("LINE_COMMENT");
  IElementType LISTING_DELIMITER = new AsciiDocElementType("LISTING_DELIMITER");
  IElementType LISTING_TEXT = new AsciiDocElementType("LISTING_TEXT");
  IElementType HEADING = new AsciiDocElementType("HEADING");

}


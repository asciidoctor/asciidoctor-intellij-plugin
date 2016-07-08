package org.asciidoc.intellij.parser;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.lexer.AsciiDocElementType;

/**
 * @author yole
 */
public interface AsciiDocElementTypes {
  IFileElementType FILE = new IFileElementType(AsciiDocLanguage.INSTANCE);
  IElementType SECTION = new AsciiDocElementType("SECTION");
  IElementType BLOCK_MACRO = new AsciiDocElementType("BLOCK_MACRO_ID");
  IElementType BLOCK = new AsciiDocElementType("BLOCK");
  IElementType BLOCK_ATTRIBUTES = new AsciiDocElementType("BLOCK_ATTRIBUTES");
  IElementType LISTING = new AsciiDocElementType("LISTING");
}

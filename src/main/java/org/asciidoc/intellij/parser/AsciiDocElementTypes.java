package org.asciidoc.intellij.parser;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.asciidoc.intellij.lexer.AsciiDocElementType;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationStub;
import org.asciidoc.intellij.psi.AsciiDocBlockId;
import org.asciidoc.intellij.psi.AsciiDocBlockIdStub;
import org.asciidoc.intellij.psi.AsciiDocBlockIdStubElementType;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocSectionStub;
import org.asciidoc.intellij.psi.AsciiDocSectionStubElementType;

/**
 * @author yole
 */
public interface AsciiDocElementTypes {
  IFileElementType FILE = new AsciiDocFileElementType();
  IElementType BLOCK_MACRO = new AsciiDocElementType("BLOCK_MACRO");
  IElementType INLINE_MACRO = new AsciiDocElementType("INLINE_MACRO");
  IElementType BLOCK = new AsciiDocElementType("BLOCK");
  IElementType BLOCK_ATTRIBUTES = new AsciiDocElementType("BLOCK_ATTRIBUTES");
  IElementType ATTRIBUTE_IN_BRACKETS = new AsciiDocElementType("ATTRIBUTE_IN_BRACKETS");
  IElementType REF = new AsciiDocElementType("REF");
  IElementType LISTING = new AsciiDocElementType("LISTING");
  IElementType FRONTMATTER = new AsciiDocElementType("FRONTMATTER");
  IElementType PASSTHROUGH = new AsciiDocElementType("PASSTHROUGH");
  IElementType LINK = new AsciiDocElementType("LINK");
  IElementType INCLUDE_TAG = new AsciiDocElementType("INCLUDE_TAG");
  IElementType ATTRIBUTE_REF = new AsciiDocElementType("ATTRIBUTE_REF");
  IElementType ATTRIBUTE_DECLARATION_NAME = new AsciiDocElementType("ATTRIBUTE_DECLARATION_NAME");
  IElementType URL = new AsciiDocElementType("URL");
  IElementType TITLE = new AsciiDocElementType("TITLE");
  IElementType MONO = new AsciiDocElementType("MONO");
  IElementType ITALIC = new AsciiDocElementType("ITALIC");
  IElementType HTML_ENTITY = new AsciiDocElementType("HTML_ENTITY");
  IStubElementType<AsciiDocAttributeDeclarationStub, AsciiDocAttributeDeclaration> ATTRIBUTE_DECLARATION = new AsciiDocAttributeDeclarationStubElementType();
  IStubElementType<AsciiDocSectionStub, AsciiDocSection> SECTION = new AsciiDocSectionStubElementType();
  IStubElementType<AsciiDocBlockIdStub, AsciiDocBlockId> BLOCKID = new AsciiDocBlockIdStubElementType();
}

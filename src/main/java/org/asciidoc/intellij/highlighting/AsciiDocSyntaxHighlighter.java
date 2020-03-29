package org.asciidoc.intellij.highlighting;

import com.google.common.collect.ImmutableMap;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.lexer.AsciiDocLexer;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocSyntaxHighlighter extends SyntaxHighlighterBase {
  static final TextAttributesKey ASCIIDOC_COMMENT = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.LINE_COMMENT",
    DefaultLanguageHighlighterColors.LINE_COMMENT
  );

  static final TextAttributesKey ASCIIDOC_LISTING_TEXT = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.LISTING_TEXT",
    DefaultLanguageHighlighterColors.MARKUP_TAG
  );

  static final TextAttributesKey ASCIIDOC_HEADING = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.HEADING",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  static final TextAttributesKey ASCIIDOC_BULLET = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.BULLET",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  static final TextAttributesKey ASCIIDOC_DESCRIPTION = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.DESCRIPTION",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  static final TextAttributesKey ASCIIDOC_CALLOUT = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.CALLOUT",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  static final TextAttributesKey ASCIIDOC_ENUMERATION = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.ENUMERATION",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  static final TextAttributesKey ASCIIDOC_BLOCK_MACRO_ID = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.BLOCK_MACRO_ID",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  static final TextAttributesKey ASCIIDOC_INLINE_MACRO_ID = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.INLINE_MACRO_ID",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  static final TextAttributesKey ASCIIDOC_BOLD = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_BOLD");

  static final TextAttributesKey ASCIIDOC_ITALIC = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_ITALIC");

  static final TextAttributesKey ASCIIDOC_BOLDITALIC = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_BOLDITALIC");

  static final TextAttributesKey ASCIIDOC_MONO = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_MONO",
    DefaultLanguageHighlighterColors.MARKUP_TAG);

  static final TextAttributesKey ASCIIDOC_MONOBOLD = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_MONOBOLD",
    DefaultLanguageHighlighterColors.MARKUP_TAG);

  static final TextAttributesKey ASCIIDOC_MONOITALIC = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_MONOITALIC",
    DefaultLanguageHighlighterColors.MARKUP_TAG);

  static final TextAttributesKey ASCIIDOC_MONOBOLDITALIC = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_MONOBOLDITALIC",
    DefaultLanguageHighlighterColors.MARKUP_TAG);

  static final TextAttributesKey ASCIIDOC_MARKER = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_MARKER",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  static final TextAttributesKey ASCIIDOC_ATTRIBUTE = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_ATTRIBUTE",
    DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE
  );

  static final TextAttributesKey ASCIIDOC_ATTRIBUTE_VAL = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_ATTRIBUTE_VAL",
    DefaultLanguageHighlighterColors.STRING
  );

  private static final ImmutableMap<IElementType, TextAttributesKey> ATTRIBUTES =
    ImmutableMap.<IElementType, TextAttributesKey>builder()
      .put(AsciiDocTokenTypes.LINE_COMMENT, ASCIIDOC_COMMENT)
      .put(AsciiDocTokenTypes.BLOCK_COMMENT, ASCIIDOC_COMMENT)
      .put(AsciiDocTokenTypes.COMMENT_BLOCK_DELIMITER, ASCIIDOC_COMMENT)
      .put(AsciiDocTokenTypes.LISTING_TEXT, ASCIIDOC_LISTING_TEXT)
      .put(AsciiDocTokenTypes.FRONTMATTER, ASCIIDOC_LISTING_TEXT)
      .put(AsciiDocTokenTypes.LITERAL_BLOCK, ASCIIDOC_LISTING_TEXT)
      .put(AsciiDocTokenTypes.HEADING, ASCIIDOC_HEADING)
      .put(AsciiDocTokenTypes.HEADING_OLDSTYLE, ASCIIDOC_HEADING)
      .put(AsciiDocTokenTypes.BOLD_END, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.BOLD_START, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.BOLD, ASCIIDOC_BOLD)
      .put(AsciiDocTokenTypes.ITALIC_END, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.ITALIC_START, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.CONTINUATION, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION_LEGACY, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.HARD_BREAK, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.ITALIC, ASCIIDOC_ITALIC)
      .put(AsciiDocTokenTypes.BOLDITALIC, ASCIIDOC_BOLDITALIC)
      .put(AsciiDocTokenTypes.MONOBOLD, ASCIIDOC_MONOBOLD)
      .put(AsciiDocTokenTypes.MONOITALIC, ASCIIDOC_MONOITALIC)
      .put(AsciiDocTokenTypes.MONOBOLDITALIC, ASCIIDOC_MONOBOLDITALIC)
      .put(AsciiDocTokenTypes.MONO_END, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.MONO_START, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.MONO, ASCIIDOC_MONO)
      .put(AsciiDocTokenTypes.WHITE_SPACE_MONO, ASCIIDOC_MONO)
      .put(AsciiDocTokenTypes.BLOCK_MACRO_ID, ASCIIDOC_BLOCK_MACRO_ID)
      .put(AsciiDocTokenTypes.INLINE_MACRO_ID, ASCIIDOC_INLINE_MACRO_ID)
      .put(AsciiDocTokenTypes.ATTRS_START, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.ATTRS_END, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.INLINE_ATTRS_START, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.INLINE_ATTRS_END, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.TITLE_TOKEN, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.BULLET, ASCIIDOC_BULLET)
      .put(AsciiDocTokenTypes.DESCRIPTION, ASCIIDOC_DESCRIPTION)
      .put(AsciiDocTokenTypes.CALLOUT, ASCIIDOC_CALLOUT)
      .put(AsciiDocTokenTypes.ENUMERATION, ASCIIDOC_ENUMERATION)
      .put(AsciiDocTokenTypes.ADMONITION, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.REFSTART, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.REFEND, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.REF, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.URL_START, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.URL_END, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.URL_PREFIX, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.LINKSTART, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.LINKANCHOR, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.LINKTEXT_START, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.LINKTEXT, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.LINKEND, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.BLOCKIDSTART, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.BLOCKIDEND, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.BLOCKID, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.BLOCK_DELIMITER, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.SEPARATOR, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.ATTRIBUTE_NAME_START, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.ATTRIBUTE_NAME, ASCIIDOC_ATTRIBUTE)
      .put(AsciiDocTokenTypes.ATTRIBUTE_UNSET, ASCIIDOC_ATTRIBUTE)
      .put(AsciiDocTokenTypes.ATTRIBUTE_NAME_END, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.ATTRIBUTE_VAL, ASCIIDOC_ATTRIBUTE_VAL)
      .put(AsciiDocTokenTypes.ATTRIBUTE_REF_START, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.ATTRIBUTE_REF, ASCIIDOC_ATTRIBUTE)
      .put(AsciiDocTokenTypes.ATTRIBUTE_REF_END, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.PASSTRHOUGH_INLINE_END, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.PAGEBREAK, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.HORIZONTALRULE, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.HEADER, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.HTML_ENTITY_OR_UNICODE, ASCIIDOC_MARKER)
      .put(AsciiDocTokenTypes.FRONTMATTER_DELIMITER, ASCIIDOC_MARKER)
      .build();

  @NotNull
  @Override
  public Lexer getHighlightingLexer() {
    return new AsciiDocLexer();
  }

  @NotNull
  @Override
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    return pack(ATTRIBUTES.get(tokenType));
  }
}

package org.asciidoc.intellij.lexer;

import com.google.common.collect.ImmutableMap;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocHighlighter extends SyntaxHighlighterBase {
  private static final TextAttributesKey ASCIIDOC_COMMENT = TextAttributesKey.createTextAttributesKey(
      "ASCIIDOC.LINE_COMMENT",
      DefaultLanguageHighlighterColors.LINE_COMMENT
  );

  private static final TextAttributesKey ASCIIDOC_LISTING_TEXT = TextAttributesKey.createTextAttributesKey(
      "ASCIIDOC.LISTING_TEXT",
      DefaultLanguageHighlighterColors.MARKUP_TAG
  );

  private static final TextAttributesKey ASCIIDOC_HEADING = TextAttributesKey.createTextAttributesKey(
      "ASCIIDOC.HEADING",
      DefaultLanguageHighlighterColors.KEYWORD
  );

  private static final TextAttributesKey ASCIIDOC_BULLET = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.BULLET",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  private static final TextAttributesKey ASCIIDOC_BLOCK_MACRO_ID = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC.BLOCK_MACRO_ID",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  private static final TextAttributesKey ASCIIDOC_BOLD = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_BOLD");

  private static final TextAttributesKey ASCIIDOC_ITALIC = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_ITALIC");

  private static final TextAttributesKey ASCIIDOC_BOLDITALIC = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_BOLDITALIC");

  private static final TextAttributesKey ASCIIDOC_MONO = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_MONO");

  private static final TextAttributesKey ASCIIDOC_MONOBOLD = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_MONOBOLD");

  private static final TextAttributesKey ASCIIDOC_MONOITALIC = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_MONOITALIC");

  private static final TextAttributesKey ASCIIDOC_MONOBOLDITALIC = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_MONOBOLDITALIC");

  private static final TextAttributesKey ASCIIDOC_MARKER = TextAttributesKey.createTextAttributesKey(
    "ASCIIDOC_MARKER",
    DefaultLanguageHighlighterColors.KEYWORD
  );

  private static final ImmutableMap<IElementType, TextAttributesKey> attributes =
      ImmutableMap.<IElementType, TextAttributesKey>builder()
          .put(AsciiDocTokenTypes.LINE_COMMENT, ASCIIDOC_COMMENT)
          .put(AsciiDocTokenTypes.BLOCK_COMMENT, ASCIIDOC_COMMENT)
          .put(AsciiDocTokenTypes.COMMENT_BLOCK_DELIMITER, ASCIIDOC_COMMENT)
          .put(AsciiDocTokenTypes.LISTING_TEXT, ASCIIDOC_LISTING_TEXT)
          .put(AsciiDocTokenTypes.QUOTE_BLOCK, ASCIIDOC_LISTING_TEXT)
          .put(AsciiDocTokenTypes.EXAMPLE_BLOCK, ASCIIDOC_LISTING_TEXT)
          .put(AsciiDocTokenTypes.SIDEBAR_BLOCK, ASCIIDOC_LISTING_TEXT)
          .put(AsciiDocTokenTypes.HEADING, ASCIIDOC_HEADING)
          .put(AsciiDocTokenTypes.HEADING_OLDSTYLE, ASCIIDOC_HEADING)
          .put(AsciiDocTokenTypes.BOLD_END, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.BOLD_START, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.BOLD, ASCIIDOC_BOLD)
          .put(AsciiDocTokenTypes.ITALIC_END, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.ITALIC_START, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.ITALIC, ASCIIDOC_ITALIC)
          .put(AsciiDocTokenTypes.BOLDITALIC, ASCIIDOC_BOLDITALIC)
          .put(AsciiDocTokenTypes.MONOBOLD, ASCIIDOC_MONOBOLD)
          .put(AsciiDocTokenTypes.MONOITALIC, ASCIIDOC_MONOITALIC)
          .put(AsciiDocTokenTypes.MONOBOLDITALIC, ASCIIDOC_MONOBOLDITALIC)
          .put(AsciiDocTokenTypes.MONO_END, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.MONO_START, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.MONO, ASCIIDOC_MONO)
          .put(AsciiDocTokenTypes.BLOCK_MACRO_ID, ASCIIDOC_BLOCK_MACRO_ID)
          .put(AsciiDocTokenTypes.BULLET, ASCIIDOC_BULLET)
          .put(AsciiDocTokenTypes.REFSTART, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.REFEND, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.REF, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.BLOCKIDSTART, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.BLOCKIDEND, ASCIIDOC_MARKER)
          .put(AsciiDocTokenTypes.BLOCKID, ASCIIDOC_MARKER)
          .build();

  @NotNull
  @Override
  public Lexer getHighlightingLexer() {
    return new AsciiDocLexer();
  }

  @NotNull
  @Override
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    return pack(attributes.get(tokenType));
  }
}

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
  public static final TextAttributesKey ASCIIDOC_COMMENT = TextAttributesKey.createTextAttributesKey(
      "ASCIIDOC.LINE_COMMENT",
      DefaultLanguageHighlighterColors.LINE_COMMENT
  );

  public static final TextAttributesKey ASCIIDOC_LISTING_TEXT = TextAttributesKey.createTextAttributesKey(
      "ASCIIDOC.LISTING_TEXT",
      DefaultLanguageHighlighterColors.MARKUP_TAG
  );

  public static final TextAttributesKey ASCIIDOC_HEADING = TextAttributesKey.createTextAttributesKey(
      "ASCIIDOC.HEADING",
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

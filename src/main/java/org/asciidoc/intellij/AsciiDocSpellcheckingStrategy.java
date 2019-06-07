package org.asciidoc.intellij;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocSpellcheckingStrategy extends SpellcheckingStrategy {
  public static TokenSet TEXT_TOKENS = TokenSet.create(
    AsciiDocTokenTypes.HEADING,
    AsciiDocTokenTypes.HEADING_OLDSTYLE,
    AsciiDocTokenTypes.TEXT,
    AsciiDocTokenTypes.ITALIC,
    AsciiDocTokenTypes.BOLD,
    AsciiDocTokenTypes.BOLDITALIC,
    AsciiDocTokenTypes.MONO,
    AsciiDocTokenTypes.MONOBOLD,
    AsciiDocTokenTypes.MONOITALIC,
    AsciiDocTokenTypes.MONOBOLDITALIC,
    AsciiDocTokenTypes.TITLE,
    AsciiDocTokenTypes.LINE_COMMENT,
    AsciiDocTokenTypes.BLOCK_COMMENT,
    AsciiDocTokenTypes.EXAMPLE_BLOCK,
    AsciiDocTokenTypes.SIDEBAR_BLOCK,
    AsciiDocTokenTypes.QUOTE_BLOCK,
    AsciiDocTokenTypes.LITERAL_BLOCK,
    AsciiDocTokenTypes.BLOCKREFTEXT,
    AsciiDocTokenTypes.LINKTEXT
  );

  @NotNull
  @Override
  public Tokenizer getTokenizer(PsiElement element) {
    IElementType elementType = element.getNode().getElementType();
    if (TEXT_TOKENS.contains(elementType)) {
      return TEXT_TOKENIZER;
    }
    return EMPTY_TOKENIZER;
  }
}

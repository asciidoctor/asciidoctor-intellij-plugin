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
  public static TokenSet TEXT_TOKENS = TokenSet.create(AsciiDocTokenTypes.HEADING,
    AsciiDocTokenTypes.TEXT,
    AsciiDocTokenTypes.BOLD,
    AsciiDocTokenTypes.ITALIC,
    AsciiDocTokenTypes.MONO,
    AsciiDocTokenTypes.BOLDITALIC,
    AsciiDocTokenTypes.MONOBOLD,
    AsciiDocTokenTypes.MONOITALIC,
    AsciiDocTokenTypes.TITLE,
    AsciiDocTokenTypes.MONOBOLDITALIC);

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

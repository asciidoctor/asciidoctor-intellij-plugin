package org.asciidoc.intellij;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocSpellcheckingStrategy extends SpellcheckingStrategy {
  @NotNull
  @Override
  public Tokenizer getTokenizer(PsiElement element) {
    IElementType elementType = element.getNode().getElementType();
    if (elementType == AsciiDocTokenTypes.HEADING || elementType == AsciiDocTokenTypes.TEXT) {
      return TEXT_TOKENIZER;
    }
    return EMPTY_TOKENIZER;
  }
}

package org.asciidoc.intellij.braces;

import com.intellij.codeInsight.highlighting.PairedBraceMatcherAdapter;
import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocBraceMatcher extends PairedBraceMatcherAdapter {

  public AsciiDocBraceMatcher() {
    super(new MyPairedBraceMatcher(), AsciiDocLanguage.INSTANCE);
  }

  private static class MyPairedBraceMatcher implements PairedBraceMatcher {

    @NotNull
    @Override
    public BracePair[] getPairs() {
      return new BracePair[]{
        new BracePair(AsciiDocTokenTypes.BLOCK_ATTRS_START, AsciiDocTokenTypes.BLOCK_ATTRS_END, true),
        new BracePair(AsciiDocTokenTypes.LPAREN, AsciiDocTokenTypes.RPAREN, false),
        new BracePair(AsciiDocTokenTypes.LBRACKET, AsciiDocTokenTypes.RBRACKET, false),
        new BracePair(AsciiDocTokenTypes.LT, AsciiDocTokenTypes.GT, false),
        new BracePair(AsciiDocTokenTypes.BOLD_START, AsciiDocTokenTypes.BOLD_END, false),
        new BracePair(AsciiDocTokenTypes.ITALIC_START, AsciiDocTokenTypes.ITALIC_END, false),
        new BracePair(AsciiDocTokenTypes.MONO_START, AsciiDocTokenTypes.MONO_END, false),
      };
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, IElementType type) {
      return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
      return openingBraceOffset;
    }
  }
}

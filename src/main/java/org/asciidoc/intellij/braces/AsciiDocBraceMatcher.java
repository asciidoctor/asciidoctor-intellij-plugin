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
        new BracePair(AsciiDocTokenTypes.ATTRS_START, AsciiDocTokenTypes.ATTRS_END, true),
        new BracePair(AsciiDocTokenTypes.LPAREN, AsciiDocTokenTypes.RPAREN, false),
        new BracePair(AsciiDocTokenTypes.LBRACKET, AsciiDocTokenTypes.RBRACKET, false),
        new BracePair(AsciiDocTokenTypes.LT, AsciiDocTokenTypes.GT, false),
        new BracePair(AsciiDocTokenTypes.BOLD_START, AsciiDocTokenTypes.BOLD_END, false),
        new BracePair(AsciiDocTokenTypes.ITALIC_START, AsciiDocTokenTypes.ITALIC_END, false),
        new BracePair(AsciiDocTokenTypes.MONO_START, AsciiDocTokenTypes.MONO_END, false),
        new BracePair(AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_START, AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_END, false),
        new BracePair(AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START, AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_END, false),
        new BracePair(AsciiDocTokenTypes.REFSTART, AsciiDocTokenTypes.REFEND, true),
        new BracePair(AsciiDocTokenTypes.BLOCKIDSTART, AsciiDocTokenTypes.BLOCKIDEND, true),
        new BracePair(AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START, AsciiDocTokenTypes.PASSTRHOUGH_INLINE_END, false),
        new BracePair(AsciiDocTokenTypes.ATTRIBUTE_NAME_START, AsciiDocTokenTypes.ATTRIBUTE_NAME_END, false),
        new BracePair(AsciiDocTokenTypes.ATTRIBUTE_REF_START, AsciiDocTokenTypes.ATTRIBUTE_REF_END, false),
        new BracePair(AsciiDocTokenTypes.BIBSTART, AsciiDocTokenTypes.BIBEND, false),
      };
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, IElementType type) {
      return type == null ||
        type == AsciiDocTokenTypes.WHITE_SPACE ||
        type == AsciiDocTokenTypes.WHITE_SPACE_MONO ||
        type == AsciiDocTokenTypes.LINE_BREAK ||
        type == AsciiDocTokenTypes.EMPTY_LINE ||
        type == AsciiDocTokenTypes.RPAREN ||
        type == AsciiDocTokenTypes.RBRACKET ||
        type == AsciiDocTokenTypes.GT;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
      return openingBraceOffset;
    }
  }
}

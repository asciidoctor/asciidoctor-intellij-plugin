package org.asciidoc.intellij.grazie;

import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;
import tanvd.grazi.grammar.GrammarChecker;
import tanvd.grazi.grammar.Typo;
import tanvd.grazi.ide.language.LanguageSupport;

import java.util.Set;

public class AsciiDocLanguageSupport extends LanguageSupport {

  // all tokens that contain full sentences that can be checked for grammar and spelling.
  private static final TokenSet NODES_TO_CHECK = TokenSet.create(
    AsciiDocTokenTypes.HEADING,
    AsciiDocTokenTypes.HEADING_OLDSTYLE,
    AsciiDocTokenTypes.TITLE_TOKEN,
    AsciiDocTokenTypes.LINE_COMMENT,
    AsciiDocTokenTypes.BLOCK_COMMENT,
    AsciiDocTokenTypes.LITERAL_BLOCK,
    AsciiDocElementTypes.SECTION,
    AsciiDocElementTypes.BLOCK
  );

  // all tokens that contain text that is part of a sentence and can be a sub-node of the elements above
  private static final TokenSet TEXT_TOKENS = TokenSet.orSet(TokenSet.create(
    AsciiDocTokenTypes.TEXT,
    AsciiDocTokenTypes.ITALIC,
    AsciiDocTokenTypes.BOLD,
    AsciiDocTokenTypes.BOLDITALIC,
    AsciiDocTokenTypes.MONO,
    AsciiDocTokenTypes.MONOBOLD,
    AsciiDocTokenTypes.DESCRIPTION,
    AsciiDocTokenTypes.LINKTEXT,
    AsciiDocTokenTypes.BLOCKREFTEXT,
    AsciiDocTokenTypes.MONOITALIC,
    AsciiDocTokenTypes.MONOBOLDITALIC,
    AsciiDocTokenTypes.END_OF_SENTENCE,
    // keep the white space in here as blanks are important to separate words
    AsciiDocTokenTypes.WHITE_SPACE,
    AsciiDocTokenTypes.WHITE_SPACE_MONO,
    TokenType.WHITE_SPACE
  ), NODES_TO_CHECK);


  @Override
  public boolean isRelevant(@NotNull PsiElement element) {
    return NODES_TO_CHECK.contains(element.getNode().getElementType());
  }

  @NotNull
  @Override
  protected Set<Typo> check(@NotNull PsiElement psiElement) {
    return GrammarChecker.Companion.getDefault().check(new PsiElement[]{psiElement}, PsiElement::getText,
      (element, index) -> {
        PsiElement elementAtIndex = element.findElementAt(index);
        if (elementAtIndex == null) {
          return false;
        }
        if (AsciiDocTokenTypes.TITLE_TOKEN.equals(elementAtIndex.getNode().getElementType()) && index == 0) {
          // ignore the leading "." at the beginning of the title
          return true;
        }
        PsiElement searchSubSection = elementAtIndex;
        while (element != searchSubSection) {
          if (NODES_TO_CHECK.contains(searchSubSection.getNode().getElementType())) {
            // don't include it here as it will be checked independently so that it is a separate sentence
            return true;
          }
          searchSubSection = searchSubSection.getParent();
        }
        return !TEXT_TOKENS.contains(elementAtIndex.getNode().getElementType());
      });
  }
}

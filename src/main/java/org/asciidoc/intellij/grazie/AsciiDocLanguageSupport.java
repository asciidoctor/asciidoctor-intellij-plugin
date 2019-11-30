package org.asciidoc.intellij.grazie;

import com.intellij.grazie.grammar.Typo;
import com.intellij.grazie.grammar.strategy.GrammarCheckingStrategy;
import com.intellij.grazie.grammar.strategy.impl.ReplaceCharRule;
import com.intellij.grazie.grammar.strategy.impl.RuleGroup;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.TokenSet;
import kotlin.ranges.IntRange;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AsciiDocLanguageSupport implements GrammarCheckingStrategy {

  // all tokens that contain full sentences that can be checked for grammar and spelling.
  private static final TokenSet NODES_TO_CHECK = TokenSet.create(
    AsciiDocTokenTypes.HEADING,
    AsciiDocTokenTypes.HEADING_OLDSTYLE,
    AsciiDocTokenTypes.TITLE_TOKEN,
    AsciiDocTokenTypes.LINE_COMMENT,
    AsciiDocTokenTypes.BLOCK_COMMENT,
    AsciiDocTokenTypes.LITERAL_BLOCK,
    AsciiDocTokenTypes.BLOCKREFTEXT,
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
    TokenType.WHITE_SPACE,
    AsciiDocElementTypes.URL, // can nest LINKTEXT
    AsciiDocElementTypes.REF, // can nest REFTEXT
    AsciiDocElementTypes.LINK // can nest LINKTEXT
  ), NODES_TO_CHECK);

  @NotNull
  @Override
  public ElementBehavior getElementBehavior(@NotNull PsiElement root, @NotNull PsiElement child) {
    if (root != child && NODES_TO_CHECK.contains(child.getNode().getElementType())) {
      return ElementBehavior.ABSORB;
    } else if (TEXT_TOKENS.contains(child.getNode().getElementType())) {
      return ElementBehavior.TEXT;
    } else {
      return ElementBehavior.STEALTH;
    }
  }

  @Nullable
  @Override
  public RuleGroup getIgnoredRuleGroup(@NotNull PsiElement root, @NotNull PsiElement child) {
    return null;
  }

  @Nullable
  @Override
  public Set<Typo.Category> getIgnoredTypoCategories(@NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
    return Collections.emptySet();
  }

  @NotNull
  @Override
  public List<ReplaceCharRule> getReplaceCharRules(@NotNull PsiElement psiElement) {
    return Collections.emptyList();
  }

  @Override
  public boolean isMyContextRoot(@NotNull PsiElement psiElement) {
    return NODES_TO_CHECK.contains(psiElement.getNode().getElementType());
  }

  @Override
  public boolean isTypoAccepted(@NotNull PsiElement psiElement, @NotNull IntRange intRange, @NotNull IntRange intRange1) {
    return true;
  }

  @NotNull
  @Override
  public LinkedHashSet<IntRange> getStealthyRanges(@NotNull PsiElement psiElement, @NotNull CharSequence charSequence) {
    return new LinkedHashSet<>();
  }
}

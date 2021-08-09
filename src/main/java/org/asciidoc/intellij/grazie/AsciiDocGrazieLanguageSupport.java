package org.asciidoc.intellij.grazie;

import com.intellij.grazie.grammar.Typo;
import com.intellij.grazie.grammar.strategy.GrammarCheckingStrategy;
import com.intellij.grazie.grammar.strategy.impl.ReplaceCharRule;
import com.intellij.grazie.grammar.strategy.impl.RuleGroup;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import kotlin.ranges.IntRange;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocPsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AsciiDocGrazieLanguageSupport implements GrammarCheckingStrategy {

  private static final Logger LOG =
    Logger.getInstance(AsciiDocGrazieLanguageSupport.class);

  private final AsciiDocLanguageSupport languageSupport = new AsciiDocLanguageSupport();

  @NotNull
  @Override
  public ElementBehavior getElementBehavior(@NotNull PsiElement root, @NotNull PsiElement child) {
    AsciiDocLanguageSupport.Behavior behavior = languageSupport.getElementBehavior(root, child);
    switch (behavior) {
      case ABSORB:
        return ElementBehavior.ABSORB;
      case STEALTH:
      case SEPARATE:
        return ElementBehavior.STEALTH;
      case TEXT:
        return ElementBehavior.TEXT;
      default:
        throw new IllegalStateException("Unexpected value: " + behavior);
    }
  }

  @Override
  public boolean isEnabledByDefault() {
    return true;
  }

  @NotNull
  @Override
  public TextDomain getContextRootTextDomain(@NotNull PsiElement root) {
    if (root instanceof PsiComment) {
      return TextDomain.COMMENTS;
    }
    return TextDomain.PLAIN_TEXT;
  }

  @SuppressWarnings("deprecation") // can't remove method using RuleGroup yet, as Java code will need to implement it
  @Nullable
  @Override
  public RuleGroup getIgnoredRuleGroup(@NotNull PsiElement root, @NotNull PsiElement child) {
    return null;
  }

  @Nullable
  @SuppressWarnings({"UnstableApiUsage", "MissingOverride", "deprecation"})
  // to be removed in 2020.2, remove @Override to keep compatibility
  // @Override
  public Set<Typo.Category> getIgnoredTypoCategories(@NotNull PsiElement psiElement, @NotNull PsiElement psiElement1) {
    return Collections.emptySet();
  }

  @SuppressWarnings({"UnstableApiUsage", "MissingOverride", "deprecation"})
  @NotNull
  // to be removed in 2020.2, remove @Override to keep compatibility
  // @Override
  public List<ReplaceCharRule> getReplaceCharRules(@NotNull PsiElement psiElement) {
    return Collections.emptyList();
  }

  @Override
  public boolean isMyContextRoot(@NotNull PsiElement psiElement) {
    return languageSupport.isMyContextRoot(psiElement);
  }

  @Override
  public boolean isTypoAccepted(@NotNull PsiElement psiElement, @NotNull IntRange intRange, @NotNull IntRange intRange1) {
    return true;
  }

  @NotNull
  @Override
  public LinkedHashSet<IntRange> getStealthyRanges(@NotNull PsiElement psiElement, @NotNull CharSequence charSequence) {
    LinkedHashSet<IntRange> ranges = new LinkedHashSet<>();
    StringBuilder parsedText = new StringBuilder();
    AsciiDocVisitor visitor = new AsciiDocVisitor() {
      private int pos = 0;
      @Override
      public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);
        @NotNull ElementBehavior elementBehavior = getElementBehavior(psiElement, element);
        if (elementBehavior != ElementBehavior.STEALTH &&
          elementBehavior != ElementBehavior.ABSORB) {
          if (element.getNode().getElementType() == AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START
            && element.getTextLength() == 2) {
            // ` at the end of '`
            ranges.add(createRange(pos + 1, pos + 1));
          }
          if (element instanceof PsiWhiteSpace && element.getTextLength() > 1 && element.getText().matches(" *")) {
            // AsciiDoc will eat extra spaces when rendering. Let's do the same here.
            ranges.add(createRange(pos, pos + element.getTextLength() - 2));
          }
          if ((element.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION
            || element.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION_LEGACY)
            && element.getTextLength() == 3) {
            // this will strip out the '+' or '\' from the continuation before forwarding it to the grammar check
            ranges.add(createRange(pos + 1, pos + 1));
          }
          if (element.getNode().getElementType() == AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_END
            && element.getTextLength() == 2) {
            // ` at the beginning of `'
            ranges.add(createRange(pos, pos));
          }
          if (element.getNode().getElementType() == AsciiDocTokenTypes.HEADING_OLDSTYLE && element.getTextLength() >= 1) {
            // ignore second line of heading
            String heading = element.getText();
            int i = heading.indexOf('\n');
            if (i != -1) {
              ranges.add(createRange(pos + i, pos + heading.length()));
            }
          }
          if (element.getNode().getElementType() == AsciiDocTokenTypes.HEADING_TOKEN && element.getTextLength() >= 1
            && element.getPrevSibling() == null) {
            // ignore "##" or "==" at start of heading
            String heading = element.getText();
            int i = 0;
            char start = heading.charAt(0);
            while (i < heading.length() && heading.charAt(i) == start) {
              ++i;
            }
            while (i < heading.length() && heading.charAt(i) == ' ') {
              ++i;
            }
            if (i > 0) {
              ranges.add(createRange(pos, pos + i - 1));
            }
          }
          PsiElement child = element.getFirstChild();
          if (child == null) {
            pos += element.getTextLength();
            parsedText.append(element.getText());
          }
          while (child != null) {
            visitElement(child);
            child = child.getNextSibling();
          }
        }
      }
    };
    visitor.visitElement(psiElement);
    if (charSequence.length() > 0 && !isSpace(charSequence.charAt(charSequence.length() - 1))) {
      // starting with 2021.2, Grazie will remove trailing spaces before calling this
      // if we find trailing spaces in the charSequences passed here, don't strip the spaces from output content as well
      while (parsedText.length() > 0 && isSpace(parsedText.charAt(parsedText.length() - 1))) {
        parsedText.setLength(parsedText.length() - 1);
      }
    }
    int removedPrefix = 0;
    if (charSequence.length() > 0 && !isSpace(charSequence.charAt(0))) {
      // starting with 2021.2, Grazie will remove leading spaces before calling this
      // if we find leading spaces in the charSequences passed here, don't strip the spaces from output content as well
      while (parsedText.length() > 0 && isSpace(parsedText.charAt(0))) {
        removedPrefix++;
        parsedText.deleteCharAt(0);
      }
    }
    LinkedHashSet<IntRange> finalRanges = new LinkedHashSet<>();
    if (!parsedText.toString().equals(charSequence.toString())) {
      LOG.error("unable to reconstruct string for grammar check", AsciiDocPsiImplUtil.getRuntimeException("didn't reconstruct string", psiElement, null,
        new Attachment("expected.txt", charSequence.toString()),
        new Attachment("actual.txt", parsedText.toString())));
    }
    for (IntRange range : ranges) {
      int endInclusive = range.getEndInclusive() - removedPrefix;
      if (endInclusive < 0) {
        continue;
      }
      int start = range.getStart() - removedPrefix;
      if (start < 0) {
        start = 0;
      }
      if (endInclusive >= charSequence.length()) {
        // strip off all ranges that fell into the whitespace removed from the end
        if (start < charSequence.length()) {
          finalRanges.add(createRange(start, charSequence.length() - 1));
        }
      } else {
        finalRanges.add(createRange(start, endInclusive));
      }
    }
    return finalRanges;
  }

  private boolean isSpace(char c) {
    return Character.isWhitespace(c) || Character.isSpaceChar(c);
  }

  private IntRange createRange(int start, int endInclusive) {
    if (start > endInclusive) {
      throw new IllegalArgumentException("start (" + start + ") is after end (" + endInclusive + "), shouldn't happen");
    }
    return new IntRange(start, endInclusive);
  }
}

package org.asciidoc.intellij.grazie;

import com.intellij.grazie.text.TextContent;
import com.intellij.grazie.text.TextContentBuilder;
import com.intellij.grazie.text.TextExtractor;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import kotlin.ranges.IntRange;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocAttributeReference;
import org.asciidoc.intellij.psi.AsciiDocPsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class AsciiDocGrazieTextExtractor extends TextExtractor {

  private static final Logger LOG =
    Logger.getInstance(AsciiDocGrazieTextExtractor.class);

  private final AsciiDocLanguageSupport languageSupport = new AsciiDocLanguageSupport();

  @Override
  protected @Nullable TextContent buildTextContent(@NotNull PsiElement root, @NotNull Set<TextContent.TextDomain> allowedDomains) {
    if (allowedDomains.contains(getContextRootTextDomain(root)) &&
      languageSupport.isMyContextRoot(root)) {
      TextContent textContent = TextContentBuilder.FromPsi
        // use this for text that is unknown and can't contain any root text
        .withUnknown(child -> child instanceof AsciiDocAttributeReference)
        // use excluding here, otherwise the contents will not be recognized as another root element
        .excluding(child ->
          languageSupport.getElementBehavior(root, child) != AsciiDocLanguageSupport.Behavior.TEXT
        )
        .build(root, getContextRootTextDomain(root));
      if (textContent != null) {
        ArrayList<IntRange> stealthyRanges = getStealthyRanges(root, textContent);
        Collections.reverse(stealthyRanges);
        for (IntRange range : stealthyRanges) {
          textContent = textContent.excludeRange(TextRange.create(range.getStart(), range.getEndInclusive() + 1));
        }
      }
      return textContent;
    }
    return null;
  }

  public TextContent.TextDomain getContextRootTextDomain(@NotNull PsiElement root) {
    if (root instanceof PsiComment) {
      return TextContent.TextDomain.COMMENTS;
    }
    return TextContent.TextDomain.PLAIN_TEXT;
  }

  @NotNull
  public ArrayList<IntRange> getStealthyRanges(@NotNull PsiElement root, @NotNull CharSequence parentText) {
    ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(root);
    TextRange rangeInElement;
    if (manipulator != null) {
      rangeInElement = manipulator.getRangeInElement(root);
    } else {
       rangeInElement = null;
    }
    ArrayList<IntRange> ranges = new ArrayList<>();
    StringBuilder parsedText = new StringBuilder();
    AsciiDocVisitor visitor = new AsciiDocVisitor() {
      private int pos = 0;
      @Override
      public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);
        if (languageSupport.getElementBehavior(root, element) == AsciiDocLanguageSupport.Behavior.TEXT) {
          if (element.getNode().getElementType() == AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START
            && element.getTextLength() == 2) {
            // ` at the end of '`
            ranges.add(createRange(pos + 1, pos + 1));
          }
          if (element instanceof PsiWhiteSpace && element.getTextLength() > 1 && element.getText().matches(" *")) {
            // AsciiDoc will eat extra spaces when rendering. Let's do the same here.
            ranges.add(createRange(pos + 1, pos + element.getTextLength() - 1));
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
            String text = element.getText();
            if (rangeInElement != null) {
              int startOfElement = root.getTextOffset() - element.getTextOffset();
              int startOfRange = rangeInElement.getStartOffset() - startOfElement;
              if (startOfElement < 0) {
                startOfRange = 0;
              }
              int endOfRange = root.getTextOffset() - element.getTextOffset() + rangeInElement.getLength();
              if (endOfRange > text.length()) {
                endOfRange = text.length();
              }
              text = TextRange.from(startOfRange, endOfRange).substring(text);
            }
            pos += text.length();
            parsedText.append(text);
          }
          while (child != null) {
            visitElement(child);
            child = child.getNextSibling();
          }
        }
      }
    };
    visitor.visitElement(root);
    // starting with 2021.2, Grazie will remove trailing spaces before calling this
    // if we find trailing spaces in the charSequences passed here, don't strip the spaces from output content as well
    while (parsedText.length() > 0 && isSpace(parsedText.charAt(parsedText.length() - 1))) {
      parsedText.setLength(parsedText.length() - 1);
    }
    int removedPrefix = 0;
    // starting with 2021.2, Grazie will remove leading spaces before calling this
    // if we find leading spaces in the charSequences passed here, don't strip the spaces from output content as well
    while (parsedText.length() > 0 && isSpace(parsedText.charAt(0))) {
      removedPrefix++;
      parsedText.deleteCharAt(0);
    }
    ArrayList<IntRange> finalRanges = new ArrayList<>();
    if (!parsedText.toString().equals(parentText.toString())) {
      LOG.error("unable to reconstruct string for grammar check", AsciiDocPsiImplUtil.getRuntimeException("didn't reconstruct string", root, null,
        new Attachment("expected.txt", parentText.toString()),
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
      if (endInclusive >= parentText.length()) {
        // strip off all ranges that fell into the whitespace removed from the end
        if (start < parentText.length()) {
          finalRanges.add(createRange(start, parentText.length() - 1));
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

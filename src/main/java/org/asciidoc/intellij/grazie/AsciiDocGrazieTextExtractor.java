package org.asciidoc.intellij.grazie;

import com.intellij.grazie.text.TextContent;
import com.intellij.grazie.text.TextContentBuilder;
import com.intellij.grazie.text.TextExtractor;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AsciiDocGrazieTextExtractor extends TextExtractor {

  private final AsciiDocLanguageSupport languageSupport = new AsciiDocLanguageSupport();

  @Override
  public @Nullable TextContent buildTextContent(@NotNull PsiElement root, @NotNull Set<TextContent.TextDomain> allowedDomains) {
    if (allowedDomains.contains(getContextRootTextDomain(root)) &&
      languageSupport.isMyContextRoot(root)) {
      TextContent textContent = TextContentBuilder.FromPsi
        // use this for text that is unknown and can't contain any root text
        .withUnknown(child ->
          languageSupport.getElementBehavior(root, child) == AsciiDocLanguageSupport.Behavior.UNKNOWN
        )
        // use excluding here, otherwise the contents will not be recognized as another root element
        .excluding(child ->
          languageSupport.getElementBehavior(root, child) != AsciiDocLanguageSupport.Behavior.TEXT
        )
        .build(root, getContextRootTextDomain(root));
      if (textContent != null && TextContent.TextDomain.PLAIN_TEXT.equals(textContent.getDomain())) {
        List<TextContent.Exclusion> stealthyRanges = getStealthyRanges(root, textContent).stream()
          .map(textRange -> new TextContent.Exclusion(textRange.getStartOffset(), textRange.getEndOffset(), false)).collect(Collectors.toList());
        textContent = textContent.excludeRanges(stealthyRanges);
      }
      return textContent;
    }
    return null;
  }

  public String summaryAsString(PsiElement root) {
    TextContent textContent = TextContentBuilder.FromPsi
      // use excluding here, otherwise the contents will not be recognized as another root element
      .excluding(child -> {
          AsciiDocLanguageSupport.Behavior elementBehavior = languageSupport.getElementBehavior(root, child);
          return elementBehavior != AsciiDocLanguageSupport.Behavior.TEXT && elementBehavior != AsciiDocLanguageSupport.Behavior.UNKNOWN;
        }
      )
      .build(root, getContextRootTextDomain(root));
    if (textContent != null) {
      List<TextContent.Exclusion> stealthyRanges = getStealthyRanges(root, textContent).stream()
        .map(textRange -> new TextContent.Exclusion(textRange.getStartOffset(), textRange.getEndOffset(), false)).collect(Collectors.toList());
      textContent = textContent.excludeRanges(stealthyRanges);
    }
    if (textContent != null) {
      textContent = textContent.trimWhitespace();
    }
    if (textContent != null) {
      return StringUtil.shortenTextWithEllipsis(textContent.toString().replaceAll("\n", " "), 50, 5);
    } else {
      return "???";
    }
  }

  public TextContent.TextDomain getContextRootTextDomain(@NotNull PsiElement root) {
    if (root instanceof PsiComment) {
      return TextContent.TextDomain.COMMENTS;
    }
    return TextContent.TextDomain.PLAIN_TEXT;
  }

  @NotNull
  public ArrayList<TextRange> getStealthyRanges(@NotNull PsiElement root, @NotNull TextContent parentText) {
    ArrayList<TextRange> ranges = new ArrayList<>();
    AsciiDocVisitor visitor = new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);
        if (languageSupport.getElementBehavior(root, element) == AsciiDocLanguageSupport.Behavior.TEXT) {
          int pos = element.getTextOffset();
          if (element.getNode().getElementType() == AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START
            && element.getTextLength() == 2) {
            // ` at the end of '`
            ranges.add(new TextRange(pos + 1, pos + 2));
          }
          if (element instanceof PsiWhiteSpace && element.getTextLength() > 1 && AsciiDocLanguageSupport.containsOnlySpaces(element)) {
            // AsciiDoc will eat extra spaces when rendering. Let's do the same here.
            ranges.add(new TextRange(pos + 1, pos + element.getTextLength()));
          }
          if ((element.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION
            || element.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION_LEGACY)
            && element.getTextLength() == 3) {
            // this will strip out the '+' or '\' and the newline from the continuation before forwarding it to the grammar check
            ranges.add(new TextRange(pos + 1, pos + 3));
          }
          if ((element.getNode().getElementType() == AsciiDocTokenTypes.DESCRIPTION_END)) {
            // this will strip the duplicated characters
            ranges.add(new TextRange(pos, pos + element.getTextLength() - 1));
          }
          if (element.getNode().getElementType() == AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_END
            && element.getTextLength() == 2) {
            // ` at the beginning of `'
            ranges.add(new TextRange(pos, pos + 1));
          }
          if (element.getNode().getElementType() == AsciiDocTokenTypes.HEADING_OLDSTYLE && element.getTextLength() >= 1) {
            // ignore second line of heading
            String heading = element.getText();
            int i = heading.indexOf('\n');
            if (i != -1) {
              ranges.add(new TextRange(pos + i, pos + heading.length()));
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
              ranges.add(new TextRange(pos, pos + i));
            }
          }
          PsiElement child = element.getFirstChild();
          while (child != null) {
            visitElement(child);
            child = child.getNextSibling();
          }
        }
      }
    };
    visitor.visitElement(root);
    ArrayList<TextRange> finalRanges = new ArrayList<>();
    for (TextRange range : ranges) {
      Integer finalStart = parentText.fileOffsetToText(range.getStartOffset());
      Integer finalEnd = parentText.fileOffsetToText(range.getEndOffset());
      if (finalStart == null || finalEnd == null) {
        continue;
      }
      finalRanges.add(new TextRange(finalStart, finalEnd));
    }
    return finalRanges;
  }

}

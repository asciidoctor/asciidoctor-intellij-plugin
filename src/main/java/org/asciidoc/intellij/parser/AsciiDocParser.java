package org.asciidoc.intellij.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Stack;

/**
 * @author yole
 */
public class AsciiDocParser implements PsiParser {
  private static class SectionMarker {
    int level;
    PsiBuilder.Marker marker;

    public SectionMarker(int level, PsiBuilder.Marker marker) {
      this.level = level;
      this.marker = marker;
    }
  }

  @NotNull
  @Override
  public ASTNode parse(@NotNull IElementType rootElementType, @NotNull PsiBuilder builder) {
    PsiBuilder.Marker root = builder.mark();

    Stack<SectionMarker> sectionStack = new Stack<SectionMarker>();
    while (!builder.eof()) {
      if (builder.getTokenType() == AsciiDocTokenTypes.HEADING) {
        int level = headingLevel(builder.getTokenText());
        while (!sectionStack.isEmpty() && sectionStack.peek().level >= level) {
          sectionStack.pop().marker.done(AsciiDocElementTypes.SECTION);
        }
        SectionMarker newMarker = new SectionMarker(level, builder.mark());
        sectionStack.push(newMarker);
      }
      else if (builder.getTokenType() == AsciiDocTokenTypes.BLOCK_MACRO_ID) {
        PsiBuilder.Marker blockMacroMark = builder.mark();
        builder.advanceLexer();
        while (builder.getTokenType() == AsciiDocTokenTypes.BLOCK_MACRO_BODY ||
            builder.getTokenType() == AsciiDocTokenTypes.BLOCK_MACRO_ATTRIBUTES) {
          builder.advanceLexer();
        }
        blockMacroMark.done(AsciiDocElementTypes.BLOCK_MACRO);
        continue;
      }
      builder.advanceLexer();
    }

    while (!sectionStack.isEmpty()) {
      sectionStack.pop().marker.done(AsciiDocElementTypes.SECTION);
    }
    root.done(rootElementType);
    return builder.getTreeBuilt();
  }

  private static int headingLevel(String headingText) {
    int result = 0;
    while (result < headingText.length() && headingText.charAt(result) == '=') {
      result++;
    }
    return result;
  }
}

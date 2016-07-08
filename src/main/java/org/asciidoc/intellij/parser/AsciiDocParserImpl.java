package org.asciidoc.intellij.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;

import java.util.Stack;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.*;

/**
 * @author yole
 */
public class AsciiDocParserImpl {
  private static class SectionMarker {
    int level;
    PsiBuilder.Marker marker;

    public SectionMarker(int level, PsiBuilder.Marker marker) {
      this.level = level;
      this.marker = marker;
    }
  }

  private final PsiBuilder myBuilder;
  private final Stack<SectionMarker> mySectionStack = new Stack<SectionMarker>();
  private PsiBuilder.Marker myBlockStartMarker = null;
  private PsiBuilder.Marker myPreBlockMarker = null;

  public AsciiDocParserImpl(PsiBuilder builder) {
    myBuilder = builder;
  }

  public void parse() {
    while (!myBuilder.eof()) {
      if (at(HEADING)) {
        dropPreBlock();
        int level = headingLevel(myBuilder.getTokenText());
        closeSections(level);
        SectionMarker newMarker = new SectionMarker(level, myBuilder.mark());
        mySectionStack.push(newMarker);
      }
      else if (at(BLOCK_MACRO_ID)) {
        PsiBuilder.Marker blockMacroMark = beginBlock();
        next();
        while (at(BLOCK_MACRO_BODY) || at(BLOCK_MACRO_ATTRIBUTES)) {
          next();
        }
        blockMacroMark.done(AsciiDocElementTypes.BLOCK_MACRO);
        continue;
      }
      else if (at(EXAMPLE_BLOCK_DELIMITER) || at(SIDEBAR_BLOCK_DELIMITER)) {
        if (myBlockStartMarker != null) {
          next();
          doneBlock();
          continue;
        }
        myBlockStartMarker = beginBlock();
      }
      else if (at(LISTING_DELIMITER)) {
        PsiBuilder.Marker listingMarker = beginBlock();
        next();
        while (at(LISTING_TEXT) || at(LISTING_DELIMITER)) {
          boolean atDelimiter = at(LISTING_DELIMITER);
          next();
          if (atDelimiter) break;
        }
        listingMarker.done(AsciiDocElementTypes.LISTING);
        continue;
      }
      else if (at(TITLE)) {
        markPreBlock();
        next();
        continue;
      }
      else if (at(BLOCK_ATTRS_START)) {
        markPreBlock();
        PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
        next();
        while (at(BLOCK_ATTR_NAME) || at(BLOCK_ATTRS_END)) {
          next();
        }
        blockAttrsMarker.done(AsciiDocElementTypes.BLOCK_ATTRIBUTES);
        continue;
      }
      dropPreBlock();
      next();
    }

    dropPreBlock();
    doneBlock();
    closeSections(0);
  }

  private void markPreBlock() {
    if (myPreBlockMarker == null) {
      myPreBlockMarker = myBuilder.mark();
    }
  }

  private void dropPreBlock() {
    if (myPreBlockMarker != null) {
      myPreBlockMarker.drop();
      myPreBlockMarker = null;
    }
  }

  private PsiBuilder.Marker beginBlock() {
    if (myPreBlockMarker != null) {
      PsiBuilder.Marker result = myPreBlockMarker;
      myPreBlockMarker = null;
      return result;
    }
    return myBuilder.mark();
  }

  private void closeSections(int level) {
    while (!mySectionStack.isEmpty() && mySectionStack.peek().level >= level) {
      mySectionStack.pop().marker.done(AsciiDocElementTypes.SECTION);
    }
  }

  private void doneBlock() {
    if (myBlockStartMarker != null) {
      myBlockStartMarker.done(AsciiDocElementTypes.BLOCK);
      myBlockStartMarker = null;
    }
  }

  private boolean at(IElementType elementType) {
    return myBuilder.getTokenType() == elementType;
  }

  private void next() {
    myBuilder.advanceLexer();
  }

  private static int headingLevel(String headingText) {
    int result = 0;
    while (result < headingText.length() && headingText.charAt(result) == '=') {
      result++;
    }
    return result;
  }
}

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
  private PsiBuilder.Marker myPreBlockMarker = null;

  public AsciiDocParserImpl(PsiBuilder builder) {
    myBuilder = builder;
  }

  public void parse() {
    myBuilder.setDebugMode(true);
    while (!myBuilder.eof()) {
      if (at(HEADING) || at(HEADING_OLDSTYLE)) {
        dropPreBlock();
        int level = headingLevel(myBuilder.getTokenText());
        closeSections(level);
        SectionMarker newMarker = new SectionMarker(level, myBuilder.mark());
        mySectionStack.push(newMarker);
      }
      else if (at(BLOCK_MACRO_ID)) {
        markPreBlock();
        next();
        while (at(BLOCK_MACRO_BODY) || at(BLOCK_MACRO_ATTRIBUTES)) {
          next();
        }
        myPreBlockMarker.done(AsciiDocElementTypes.BLOCK_MACRO);
        myPreBlockMarker = null;
        continue;
      }
      else if (at(EXAMPLE_BLOCK_DELIMITER) || at(SIDEBAR_BLOCK_DELIMITER) || at(QUOTE_BLOCK_DELIMITER)
          || at(LISTING_BLOCK_DELIMITER) || at(COMMENT_BLOCK_DELIMITER) || at(PASSTRHOUGH_BLOCK_DELIMITER)) {
        parseBlock();
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
    closeSections(0);
  }

  private void parseBlock() {
    PsiBuilder.Marker myBlockStartMarker;
    if (myPreBlockMarker != null) {
      myBlockStartMarker = myPreBlockMarker;
      myPreBlockMarker = null;
    }
    else {
      myBlockStartMarker = beginBlock();
    }
    String marker = myBuilder.getTokenText();
    IElementType type = myBuilder.getTokenType();
    next();
    while (true) {
      if (myBuilder.eof()) {
        myBlockStartMarker.done(AsciiDocElementTypes.BLOCK);
        break;
      }
      // the block needs to be terminated by the same sequence that started it
      if (at(type) && myBuilder.getTokenText().equals(marker)) {
        next();
        myBlockStartMarker.done(AsciiDocElementTypes.BLOCK);
        break;
      }
      next();
    }
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
    return myBuilder.mark();
  }

  private void closeSections(int level) {
    while (!mySectionStack.isEmpty() && mySectionStack.peek().level >= level) {
      mySectionStack.pop().marker.done(AsciiDocElementTypes.SECTION);
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
    if (result == 0) {
      // this is old header style
      switch (headingText.charAt(headingText.length() - 2)) {
        case '+':
          ++ result;
        case '^':
          ++ result;
        case '~':
          ++ result;
        case '-':
          ++ result;
        case '=':
          ++ result;
      }
    }
    return result;
  }
}

package org.asciidoc.intellij.parser;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKID;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKIDEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKIDSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKREFTEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_ATTRS_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_ATTRS_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_ATTR_NAME;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_ATTR_VALUE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_MACRO_ATTRIBUTES;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_MACRO_BODY;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_MACRO_ID;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.CODE_FENCE_CONTENT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.COMMENT_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HEADING;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HEADING_OLDSTYLE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINE_BREAK;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKANCHOR;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKFILE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKTEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKTEXT_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REF;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFFILE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFTEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.SEPARATOR;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.TITLE;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import java.util.Stack;

/**
 * @author yole
 */
public class AsciiDocParserImpl {
  private static class SectionMarker {
    int level;
    PsiBuilder.Marker marker;

    SectionMarker(int level, PsiBuilder.Marker marker) {
      this.level = level;
      this.marker = marker;
    }

  }

  private static class BlockMarker {
    String delimiter;
    PsiBuilder.Marker marker;

    BlockMarker(String delimiter, PsiBuilder.Marker marker) {
      this.delimiter = delimiter;
      this.marker = marker;
    }

  }

  private final PsiBuilder myBuilder;
  private final Stack<SectionMarker> mySectionStack = new Stack<>();
  private final Stack<BlockMarker> myBlockMarker = new Stack<>();
  private PsiBuilder.Marker myPreBlockMarker = null;

  public AsciiDocParserImpl(PsiBuilder builder) {
    myBuilder = builder;
  }

  int newLines;

  public void parse() {
    myBuilder.setDebugMode(true);
    myBuilder.setWhitespaceSkippedCallback((type, start, end) -> {
      if(type == LINE_BREAK) {
        ++newLines;
      }
    });
    while (!myBuilder.eof()) {
      if ((at(HEADING) || at(HEADING_OLDSTYLE)) && myBlockMarker.size() == 0) {
        int level = headingLevel(myBuilder.getTokenText());
        closeSections(level);
        PsiBuilder.Marker marker;
        if(myPreBlockMarker != null) {
          marker = myPreBlockMarker;
          myPreBlockMarker = null;
        } else {
          marker = myBuilder.mark();
        }
        SectionMarker newMarker = new SectionMarker(level, marker);
        mySectionStack.push(newMarker);
      } else if (at(BLOCK_MACRO_ID)) {
        newLines = 0;
        markPreBlock();
        next();
        while ((at(BLOCK_MACRO_BODY) || at(BLOCK_MACRO_ATTRIBUTES) || at(BLOCK_ATTRS_START) || at(BLOCK_ATTRS_END))
          && newLines == 0) {
          if (at(BLOCK_ATTRS_END)) {
            next();
            break;
          }
          next();
        }
        myPreBlockMarker.done(AsciiDocElementTypes.BLOCK_MACRO);
        myPreBlockMarker = null;
        continue;
      } else if (at(BLOCK_DELIMITER)
          || at(COMMENT_BLOCK_DELIMITER) || at(PASSTRHOUGH_BLOCK_DELIMITER) || at(LITERAL_BLOCK_DELIMITER)) {
        parseBlock();
        continue;
      } else if (at(LISTING_BLOCK_DELIMITER)) {
        parseListing();
        continue;
      } else if (at(TITLE)) {
        markPreBlock();
        next();
        continue;
      } else if (at(BLOCK_ATTRS_START)) {
        markPreBlock();
        PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
        next();
        while (at(BLOCK_ATTR_NAME) || at(BLOCK_ATTR_VALUE) || at(BLOCK_ATTRS_END) || at(SEPARATOR)) {
          next();
        }
        blockAttrsMarker.done(AsciiDocElementTypes.BLOCK_ATTRIBUTES);
        continue;
      } else if (at(BLOCKIDSTART)) {
        markPreBlock();
        next();
        while (at(BLOCKID) || at(BLOCKIDEND) || at(SEPARATOR) || at(BLOCKREFTEXT)) {
          if(at(BLOCKID)) {
            PsiBuilder.Marker blockIdMarker = myBuilder.mark();
            next();
            blockIdMarker.done(AsciiDocElementTypes.BLOCKID);
          } else {
            next();
          }
        }
        continue;
      } else if (at(REFSTART)) {
        PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
        next();
        while (at(REF) || at(REFEND) || at(REFFILE) || at(SEPARATOR) || at(REFTEXT)) {
          next();
        }
        blockAttrsMarker.done(AsciiDocElementTypes.REF);
        continue;
      } else if (at(LINKSTART)) {
        PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
        next();
        while (at(LINKFILE) || at(LINKANCHOR) || at(LINKTEXT_START) || at(SEPARATOR) || at(LINKTEXT) || at(LINKEND)) {
          next();
        }
        blockAttrsMarker.done(AsciiDocElementTypes.LINK);
        continue;
      }

      dropPreBlock();
      next();
    }

    dropPreBlock();
    closeBlocks();
    closeSections(0);
  }

  private void closeBlocks() {
    while (!myBlockMarker.isEmpty()) {
      PsiBuilder.Marker marker = myBlockMarker.pop().marker;
      marker.done(AsciiDocElementTypes.BLOCK);
    }
  }

  private void parseBlock() {
    String delimiter = myBuilder.getTokenText();
    if (delimiter == null) {
      return;
    }
    delimiter = delimiter.trim();
    if (myBlockMarker.size() > 0 && myBlockMarker.peek().delimiter.equals(delimiter)) {
      dropPreBlock();
      next();
      BlockMarker currentBlock = myBlockMarker.pop();
      currentBlock.marker.done(AsciiDocElementTypes.BLOCK);
    } else {
      PsiBuilder.Marker myBlockStartMarker;
      if (myPreBlockMarker != null) {
        myBlockStartMarker = myPreBlockMarker;
        myPreBlockMarker = null;
      } else {
        myBlockStartMarker = beginBlock();
      }
      myBlockMarker.push(new BlockMarker(delimiter, myBlockStartMarker));
      next();
    }
  }

  private void parseListing() {
    PsiBuilder.Marker myBlockStartMarker;
    if (myPreBlockMarker != null) {
      myBlockStartMarker = myPreBlockMarker;
      myPreBlockMarker = null;
    } else {
      myBlockStartMarker = beginBlock();
    }
    String marker = myBuilder.getTokenText();
    IElementType type = myBuilder.getTokenType();
    next();
    PsiBuilder.Marker myCodeBlockStart = beginBlock();
    PsiBuilder.Marker myCodeBlockEnd = null;
    while (true) {
      if (myBuilder.eof()) {
        break;
      }
      // the block needs to be terminated by the same sequence that started it
      if (at(type) && myBuilder.getTokenText() != null && myBuilder.getTokenText().equals(marker)) {
        next();
        break;
      }
      if (at(BLOCK_MACRO_ID)) {
        newLines = 0;
        PsiBuilder.Marker blockMacro = myBuilder.mark();
        next();
        while (at(BLOCK_MACRO_BODY) || at(BLOCK_MACRO_ATTRIBUTES) || at(BLOCK_ATTRS_START) || at(BLOCK_ATTRS_END)
          && newLines == 0) {
          if (at(BLOCK_ATTRS_END)) {
            next();
            break;
          }
          next();
        }
        blockMacro.done(AsciiDocElementTypes.BLOCK_MACRO);
        if (myCodeBlockEnd != null) {
          myCodeBlockEnd.drop();
        }
        myCodeBlockEnd = myBuilder.mark();
        continue;
      }

      if (myCodeBlockEnd != null) {
        myCodeBlockEnd.drop();
      }
      next();
      myCodeBlockEnd = myBuilder.mark();
    }
    if (myCodeBlockEnd != null) {
      myCodeBlockStart.doneBefore(CODE_FENCE_CONTENT, myCodeBlockEnd);
      myCodeBlockEnd.drop();
    } else {
      myCodeBlockStart.drop();
    }
    myBlockStartMarker.done(AsciiDocElementTypes.LISTING);
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
      PsiBuilder.Marker marker = mySectionStack.pop().marker;
      if(myPreBlockMarker != null) {
        marker.doneBefore(AsciiDocElementTypes.SECTION, myPreBlockMarker);
      } else {
        marker.done(AsciiDocElementTypes.SECTION);
      }
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
    while (result < headingText.length() && (headingText.charAt(result) == '=' || headingText.charAt(result) == '#')) {
      result++;
    }
    if (result == 0) {
      // this is old header style
      char character = headingText.charAt(headingText.length() - 2);
      switch (character) {
        case '+':
          ++result;
        case '^':
          ++result;
        case '~':
          ++result;
        case '-':
          ++result;
        case '=':
          ++result;
          break;
        default:
          throw new IllegalArgumentException("unknown character '" + character + "' in switch");
      }
    }
    return result;
  }
}

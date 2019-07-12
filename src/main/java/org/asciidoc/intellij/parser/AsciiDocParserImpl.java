package org.asciidoc.intellij.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_NAME;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_NAME_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_NAME_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_REF;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_REF_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_REF_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_VAL;
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
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.COMMENT_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HEADING;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HEADING_OLDSTYLE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_ATTRS_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_ATTRS_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_ATTR_NAME;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_ATTR_VALUE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_MACRO_BODY;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_MACRO_ID;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINE_BREAK;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKANCHOR;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKFILE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKTEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKTEXT_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LISTING_TEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REF;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFFILE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFTEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.SEPARATOR;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.TITLE;

/**
 * @author yole
 */
public class AsciiDocParserImpl {
  private static class SectionMarker {
    @SuppressWarnings("checkstyle:visibilitymodifier")
    final int level;
    @SuppressWarnings("checkstyle:visibilitymodifier")
    final PsiBuilder.Marker marker;

    SectionMarker(int level, PsiBuilder.Marker marker) {
      this.level = level;
      this.marker = marker;
    }

  }

  private static class BlockMarker {
    @SuppressWarnings("checkstyle:visibilitymodifier")
    final String delimiter;
    @SuppressWarnings("checkstyle:visibilitymodifier")
    final PsiBuilder.Marker marker;

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

  private int newLines;

  public void parse() {
    myBuilder.setDebugMode(true);
    myBuilder.setWhitespaceSkippedCallback((type, start, end) -> {
      if (type == LINE_BREAK) {
        ++newLines;
      }
    });
    while (!myBuilder.eof()) {
      if ((at(HEADING) || at(HEADING_OLDSTYLE)) && myBlockMarker.size() == 0) {
        int level = headingLevel(myBuilder.getTokenText());
        closeSections(level);
        PsiBuilder.Marker marker;
        if (myPreBlockMarker != null) {
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
        while ((at(BLOCK_MACRO_BODY) || at(BLOCK_ATTR_NAME) || at(BLOCK_ATTR_VALUE) || at(SEPARATOR) || at(BLOCK_ATTRS_START) || at(BLOCK_ATTRS_END))
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
      } else if (at(INLINE_MACRO_ID)) {
        newLines = 0;
        PsiBuilder.Marker inlineMacroMarker = myBuilder.mark();
        next();
        while ((at(INLINE_MACRO_BODY) || at(INLINE_ATTR_NAME) || at(INLINE_ATTR_VALUE) || at(SEPARATOR) || at(INLINE_ATTRS_START) || at(INLINE_ATTRS_END))
          && newLines == 0) {
          if (at(INLINE_ATTRS_END)) {
            next();
            break;
          }
          next();
        }
        inlineMacroMarker.done(AsciiDocElementTypes.INLINE_MACRO);
        continue;
      } else if (at(BLOCK_DELIMITER)
        || at(COMMENT_BLOCK_DELIMITER) || at(PASSTRHOUGH_BLOCK_DELIMITER) || at(LITERAL_BLOCK_DELIMITER)) {
        parseBlock();
        continue;
      } else if (at(LISTING_BLOCK_DELIMITER)) {
        parseListing();
        continue;
      } else if (at(LISTING_TEXT)) {
        parseListingNoDelimiter();
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
          if (at(BLOCKID)) {
            PsiBuilder.Marker blockIdMarker = myBuilder.mark();
            next();
            blockIdMarker.done(AsciiDocElementTypes.BLOCKID);
          } else {
            next();
          }
        }
        continue;
      }

      if (myPreBlockMarker != null) {
        startBlockNoDelimiter();
      }

      if (at(REFSTART)) {
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
      } else if (at(ATTRIBUTE_NAME_START)) {
        PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
        next();
        while (at(ATTRIBUTE_NAME) || at(ATTRIBUTE_NAME_END) || at(ATTRIBUTE_VAL)) {
          if (at(ATTRIBUTE_NAME)) {
            PsiBuilder.Marker blockIdMarker = myBuilder.mark();
            next();
            blockIdMarker.done(AsciiDocElementTypes.ATTRIBUTE_DECLARATION_NAME);
          } else {
            next();
          }
        }
        blockAttrsMarker.done(AsciiDocElementTypes.ATTRIBUTE_DECLARATION);
        continue;
      } else if (at(ATTRIBUTE_REF_START)) {
        PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
        next();
        while (at(ATTRIBUTE_REF) || at(ATTRIBUTE_REF_END)) {
          next();
        }
        blockAttrsMarker.done(AsciiDocElementTypes.ATTRIBUTE_REF);
        continue;
      }

      dropPreBlock();

      newLines = 0;
      next();
      // call eof to trigger skipWhitespace
      if (!myBuilder.eof() && newLines > 1) {
        endBlockNoDelimiter();
      }
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
    boolean existsInStack = false;
    for (BlockMarker m : myBlockMarker) {
      if (m.delimiter.equals(delimiter)) {
        existsInStack = true;
        break;
      }
    }
    if (existsInStack) {
      dropPreBlock();
      // close all non-matching blocks
      while (!myBlockMarker.peek().delimiter.equals(delimiter)) {
        BlockMarker currentBlock = myBlockMarker.pop();
        currentBlock.marker.done(AsciiDocElementTypes.BLOCK);
      }
      next();
      // close this block
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

  private void startBlockNoDelimiter() {
    PsiBuilder.Marker myBlockStartMarker;
    if (myPreBlockMarker != null) {
      myBlockStartMarker = myPreBlockMarker;
      myPreBlockMarker = null;
    } else {
      myBlockStartMarker = beginBlock();
    }
    myBlockMarker.push(new BlockMarker("nodel", myBlockStartMarker));
  }

  private void endBlockNoDelimiter() {
    if (myBlockMarker.size() > 0 && myBlockMarker.peek().delimiter.equals("nodel")) {
      dropPreBlock();
      BlockMarker currentBlock = myBlockMarker.pop();
      currentBlock.marker.done(AsciiDocElementTypes.BLOCK);
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
        continue;
      }
      next();
    }
    myBlockStartMarker.done(AsciiDocElementTypes.LISTING);
  }

  private void parseListingNoDelimiter() {
    PsiBuilder.Marker myBlockStartMarker;
    if (myPreBlockMarker != null) {
      myBlockStartMarker = myPreBlockMarker;
      myPreBlockMarker = null;
    } else {
      myBlockStartMarker = beginBlock();
    }
    while (true) {
      if (myBuilder.eof()) {
        break;
      }
      if (!at(LISTING_TEXT)) {
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
        continue;
      }
      newLines = 0;
      next();
      // eof() triggers skipWhitepace that increments newLines
      if (!myBuilder.eof() && newLines > 1) {
        break;
      }
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
      if (myPreBlockMarker != null) {
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

  private static int headingLevel(@Nullable String headingText) {
    if (headingText == null) {
      return 0;
    }
    int result = 0;
    while (result < headingText.length() && (headingText.charAt(result) == '=' || headingText.charAt(result) == '#')) {
      result++;
    }
    if (result == 0) {
      // this is old header style
      int pos = headingText.length() - 1;
      while (pos > 0 && (headingText.charAt(pos) == ' ' || headingText.charAt(pos) == '\t')) {
        --pos;
      }
      char character = headingText.charAt(pos);
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

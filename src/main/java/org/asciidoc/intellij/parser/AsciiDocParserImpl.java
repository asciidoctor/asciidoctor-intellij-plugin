package org.asciidoc.intellij.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ASSIGNMENT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION_LEGACY;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_NAME;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_NAME_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_NAME_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_REF;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_REF_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_REF_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_SOFTSET;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_UNSET;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRIBUTE_VAL;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRS_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRS_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTR_LIST_OP;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTR_LIST_SEP;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTR_NAME;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTR_VALUE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BIBEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BIBSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKID;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKIDEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKIDSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKREFTEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_COMMENT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_MACRO_BODY;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_MACRO_ID;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BOLD;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BOLDITALIC;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BULLET;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.CALLOUT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.CELLSEPARATOR;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.COMMENT_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.CONTINUATION;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.DESCRIPTION;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.DESCRIPTION_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.DOUBLE_QUOTE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.EMPTY_LINE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ENUMERATION;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.FRONTMATTER_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.GT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HEADER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HEADING_OLDSTYLE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HEADING_TOKEN;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HORIZONTALRULE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.HTML_ENTITY_OR_UNICODE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINEIDEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINEIDSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_ATTRS_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_ATTRS_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_MACRO_BODY;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_MACRO_ID;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ITALIC;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LBRACKET;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINE_BREAK;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINE_COMMENT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKANCHOR;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKFILE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINKSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LISTING_TEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LPAREN;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.MACROTEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.MONO;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.MONOBOLD;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.MONOBOLDITALIC;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.MONOITALIC;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.PAGEBREAK;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.PASSTRHOUGH_CONTENT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.PASSTRHOUGH_INLINE_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.RBRACKET;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REF;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.REFTEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.RPAREN;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.SEPARATOR;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.SINGLE_QUOTE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.TEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.TITLE_TOKEN;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.URL_EMAIL;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.URL_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.URL_LINK;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.URL_PREFIX;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.URL_START;
import static org.asciidoc.intellij.parser.AsciiDocElementTypes.DESCRIPTION_ITEM;
import static org.asciidoc.intellij.parser.AsciiDocElementTypes.LIST;
import static org.asciidoc.intellij.parser.AsciiDocElementTypes.LIST_ITEM;
import static org.asciidoc.intellij.psi.AsciiDocTextQuoted.ALLQUOTES;
import static org.asciidoc.intellij.psi.AsciiDocTextQuoted.QUOTEPAIRS;

/**
 * @author yole
 */
@SuppressWarnings("JdkObsolete") // this class still uses "Stack", and ErrorProne complains about it
public class AsciiDocParserImpl {
  private static final com.intellij.openapi.diagnostic.Logger LOG =
    com.intellij.openapi.diagnostic.Logger.getInstance(AsciiDocParserImpl.class);

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
    @SuppressWarnings("checkstyle:visibilitymodifier")
    final IElementType type;

    BlockMarker(String delimiter, PsiBuilder.Marker marker, IElementType type) {
      this.delimiter = delimiter;
      this.marker = marker;
      this.type = type;
    }

  }

  private final PsiBuilder myBuilder;
  @SuppressWarnings("JdkObsolete")
  private final Stack<SectionMarker> mySectionStack = new Stack<>();
  @SuppressWarnings("JdkObsolete")
  private final Stack<BlockMarker> myBlockMarker = new Stack<>();
  private PsiBuilder.Marker myPreBlockMarker = null;

  AsciiDocParserImpl(PsiBuilder builder) {
    myBuilder = builder;
  }

  private int newLines;
  private int emptyLines;
  private int commentAfterEmptyLine;

  @SuppressWarnings("checkstyle:MethodLength")
  public void parse() {
    myBuilder.setDebugMode(ApplicationManager.getApplication().isUnitTestMode());
    if (LOG.isDebugEnabled()) {
      myBuilder.setDebugMode(true);
      if (LOG.isTraceEnabled()) {
        LOG.trace("text to be parsed:\n" + myBuilder.getOriginalText());
      }
    }
    myBuilder.setWhitespaceSkippedCallback((type, start, end) -> {
      if (type == EMPTY_LINE) {
        emptyLines++;
      }
      if (type == LINE_BREAK || type == EMPTY_LINE) {
        newLines++;
      }
      if (type == EMPTY_LINE) {
        IElementType nextToken = myBuilder.rawLookup(1);
        if (nextToken == LINE_COMMENT || nextToken == BLOCK_COMMENT) {
          commentAfterEmptyLine++;
        }
      }
    });
    boolean continuation = false;

    while (!myBuilder.eof()) {
      if (emptyLines > 0) {
        endBlockNoDelimiter();
      }
      if (emptyLines > 0 && !at(ENUMERATION) && !at(BULLET) && !at(DESCRIPTION) && !at(CONTINUATION)) {
        endListDelimiter();
      } else if (commentAfterEmptyLine > 0) {
        endListDelimiter();
      }
      emptyLines = 0;
      commentAfterEmptyLine = 0;

      if (at(BLOCK_MACRO_ID) || at(BLOCK_DELIMITER) || at(LITERAL_BLOCK_DELIMITER) || at(LISTING_BLOCK_DELIMITER)
        || at(PASSTRHOUGH_BLOCK_DELIMITER) || at(FRONTMATTER_DELIMITER) || at(CELLSEPARATOR)) {
        if (!continuation) {
          endListDelimiter();
          endBlockNoDelimiter();
        } else {
          continuation = false;
        }
      }

      if (at(HEADING_TOKEN) || at(HEADING_OLDSTYLE)) {
        parseHeading();
        continue;
      } else if (at(BLOCK_MACRO_ID)) {
        parseBlockMacro();
        continue;
      } else if (at(COMMENT_BLOCK_DELIMITER)) {
        endListDelimiter();
        parseBlock();
        continue;
      } else if (at(BLOCK_DELIMITER) || at(COMMENT_BLOCK_DELIMITER)) {
        parseBlock();
        continue;
      } else if (at(LITERAL_BLOCK_DELIMITER)) {
        parseBlockElement(AsciiDocElementTypes.LISTING);
        continue;
      } else if (at(LISTING_BLOCK_DELIMITER)) {
        parseBlockElement(AsciiDocElementTypes.LISTING);
        continue;
      } else if (at(FRONTMATTER_DELIMITER)) {
        parseBlockElement(AsciiDocElementTypes.FRONTMATTER);
        continue;
      } else if (at(PASSTRHOUGH_BLOCK_DELIMITER)) {
        parseBlockElement(AsciiDocElementTypes.PASSTHROUGH);
        continue;
      } else if (at(LISTING_TEXT)) {
        parseListingNoDelimiter();
        continue;
      } else if (at(TITLE_TOKEN)) {
        parseTitle();
        continue;
      } else if (at(HTML_ENTITY_OR_UNICODE)) {
        parseHtmlEntityOrUnicode();
        continue;
      } else if (at(ATTRIBUTE_NAME_START)) {
        parseAttributeDeclaration();
        continue;
      } else if (at(ATTRS_START)) {
        parseBlockAttributes();
        continue;
      } else if (at(BLOCKIDSTART)) {
        parseBlockId();
        continue;
      } else if (at(CELLSEPARATOR)) {
        parseBlock();
        continue;
      } else if (at(CONTINUATION)) {
        newLines = 0;
        next();
        if (!myBuilder.eof() && newLines > 2) {
          // a continuation might have one blank line, but not two blank lines
          endListDelimiter();
          continuation = false;
        } else {
          continuation = true;
        }
        continue;
      }
      continuation = false;

      if (at(LINE_COMMENT)) {
        endListDelimiter();
      }

      if (at(ENUMERATION) || at(BULLET) || at(DESCRIPTION_END) || at(CALLOUT)) {
        startEnumerationDelimiter();
      }

      // those tokens shouldn't end wrapped in a block
      if (at(PAGEBREAK) || at(HEADER) || at(HORIZONTALRULE)) {
        next();
        continue;
      }

      if (at(DESCRIPTION)) {
        markPreBlock();
      } else if (myBlockMarker.size() == 0 || (myPreBlockMarker != null && myBuilder.rawLookup(((PsiBuilderImpl.ProductionMarker) myPreBlockMarker).getStartIndex() - myBuilder.rawTokenIndex()) != DESCRIPTION)) {
        startBlockNoDelimiter();
      }

      if (at(URL_START) || at(URL_LINK) || at(URL_EMAIL) || at(URL_PREFIX)) {
        parseUrl();
      } else if (at(INLINEIDSTART)) {
        parseInlineId();
      } else if (at(INLINE_MACRO_ID)) {
        parseInlineMacro();
      } else if (at(REFSTART)) {
        parseRef();
      } else if (at(BIBSTART)) {
        parseBib();
      } else if (at(LINKSTART)) {
        parseLink();
      } else if (QUOTEPAIRS.containsKey(myBuilder.getTokenType())) {
        parseQuoted(new HashSet<>());
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }

    dropPreBlock();
    closeBlocks();
    closeSections(0);
  }

  private void closeBlockMarker(PsiBuilder.Marker myPreBlockId) {
    BlockMarker currentBlock = myBlockMarker.pop();
    currentBlock.marker.doneBefore(currentBlock.type, myPreBlockId);
  }


  private void parseHeading() {
    int level = headingLevel(myBuilder.getTokenText());
    // if we're at a heading, ensure to close all previous blocks before closing sections
    while (!myBlockMarker.isEmpty()) {
      if (myPreBlockMarker != null) {
        closeBlockMarker(myPreBlockMarker);
      } else {
        closeBlockMarker();
      }
    }
    // the following needs myPreBlockMarker to be set, therfore execute it before resetting myPreBlockMarker
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

    PsiBuilder.Marker heading = myBuilder.mark();
    newLines = 0;
    while ((at(HEADING_TOKEN) || at(HEADING_OLDSTYLE) || at(INLINEIDSTART) || at(ATTRIBUTE_REF_START))
      && newLines == 0) {
      if (at(INLINEIDSTART)) {
        parseInlineId();
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
    heading.done(AsciiDocElementTypes.HEADING);
  }

  private void parseId() {
    PsiBuilder.Marker blockIdMarker = null;
    while (at(BLOCKID) || at(ATTRIBUTE_REF_START)) {
      if (blockIdMarker == null) {
        blockIdMarker = myBuilder.mark();
      }
      if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
    if (blockIdMarker != null) {
      blockIdMarker.done(AsciiDocElementTypes.BLOCKID);
    }
  }

  private void parseBlockId() {
    markPreBlock();
    next();
    PsiBuilder.Marker blockIdMarker = null;
    while (at(BLOCKID) || at(ATTRIBUTE_REF_START)) {
      if (blockIdMarker == null) {
        blockIdMarker = myBuilder.mark();
      }
      if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
    if (blockIdMarker != null) {
      blockIdMarker.done(AsciiDocElementTypes.BLOCKID);
    }
    while (at(BLOCKIDEND) || at(SEPARATOR) || at(BLOCKREFTEXT) || at(ATTRIBUTE_REF_START)) {
      if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
  }

  private void parseInlineId() {
    next();
    PsiBuilder.Marker blockIdMarker = null;
    while (at(BLOCKID) || at(ATTRIBUTE_REF_START)) {
      if (blockIdMarker == null) {
        blockIdMarker = myBuilder.mark();
      }
      if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
    if (blockIdMarker != null) {
      blockIdMarker.done(AsciiDocElementTypes.BLOCKID);
    }
    while (at(INLINEIDEND) || at(SEPARATOR) || at(BLOCKREFTEXT) || at(ATTRIBUTE_REF_START)) {
      if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
  }

  private void parseBib() {
    while (at(BIBSTART) || at(BIBEND) || at(SEPARATOR) || at(BLOCKREFTEXT) || at(BLOCKID) || at(ATTRIBUTE_REF_START)) {
      if (at(BLOCKID)) {
        // tests show: IDs in the bibliography can't contain attribute references
        PsiBuilder.Marker blockIdMarker = myBuilder.mark();
        next();
        blockIdMarker.done(AsciiDocElementTypes.BLOCKID);
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
  }

  private void parseRef() {
    PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
    next();
    while (at(REF) || at(REFEND) || at(SEPARATOR) || at(REFTEXT) || at(ATTRIBUTE_REF_START)) {
      if (at(REFEND)) {
        next();
        break;
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
    blockAttrsMarker.done(AsciiDocElementTypes.REF);
  }

  private void parseLink() {
    PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
    String macroId = null;
    if (at(LINKSTART)) {
      macroId = myBuilder.getTokenText();
    }
    next();
    while (at(LINKFILE) || at(URL_LINK) || at(LINKANCHOR) || at(INLINE_ATTRS_START) || at(SEPARATOR) || at(MACROTEXT) || at(ATTR_NAME)
      || at(ATTRIBUTE_REF_START) || at(CONTINUATION) || at(INLINE_ATTRS_END)) {
      if (at(INLINE_ATTRS_END)) {
        next();
        break;
      } else if (at(ATTR_NAME)) {
        parseAttributeInBrackets(macroId);
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
    blockAttrsMarker.done(AsciiDocElementTypes.LINK);
  }

  private void parseHtmlEntityOrUnicode() {
    PsiBuilder.Marker marker = myBuilder.mark();
    next();
    marker.done(AsciiDocElementTypes.HTML_ENTITY);
  }

  private void parseQuoted(Set<IElementType> tokensToStop) {
    PsiBuilder.Marker quoteMarker = myBuilder.mark();
    IElementType quote = myBuilder.getTokenType();
    IElementType endQuote = QUOTEPAIRS.get(quote);
    next();
    while ((at(MONO) || at(BOLD) || at(TEXT) || at(MONOITALIC) || at(MONOBOLDITALIC) || at(MONOBOLD) ||
      at(ITALIC) || at(BOLDITALIC) || ALLQUOTES.contains(myBuilder.getTokenType()) ||
      at(ATTRIBUTE_REF_START) || at(INLINE_MACRO_ID) ||
      at(PASSTRHOUGH_CONTENT) ||
      at(LBRACKET) || at(RBRACKET) || at(LT) || at(GT) || at(LPAREN) || at(RPAREN) ||
      at(URL_START) || at(URL_LINK) || at(URL_EMAIL) || at(URL_PREFIX) ||
      at(INLINEIDSTART) || at(REFSTART) || at(BIBSTART) || at(LINKSTART))
      && emptyLines == 0) {
      if (at(endQuote)) {
        next();
        quoteMarker.done(AsciiDocElementTypes.QUOTED);
        quoteMarker = null;
        break;
      } else if (tokensToStop.contains(myBuilder.getTokenType())) {
        break;
      } else if (QUOTEPAIRS.get(myBuilder.getTokenType()) != null) {
        HashSet<IElementType> childSetToStop = new HashSet<>(tokensToStop);
        childSetToStop.add(endQuote);
        parseQuoted(childSetToStop);
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else if (at(INLINE_MACRO_ID)) {
        parseInlineMacro();
      } else if (at(URL_START) || at(URL_LINK) || at(URL_EMAIL) || at(URL_PREFIX)) {
        parseUrl();
      } else if (at(INLINEIDSTART)) {
        parseInlineId();
      } else if (at(REFSTART)) {
        parseRef();
      } else if (at(BIBSTART)) {
        parseBib();
      } else if (at(LINKSTART)) {
        parseLink();
      } else {
        next();
      }
    }
    if (quoteMarker != null) {
      quoteMarker.drop();
    }
  }

  private void parseBlockAttributes() {
    markPreBlock();
    PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
    next();
    while (at(ATTR_NAME) || at(ATTR_VALUE) || at(ATTRS_END) || at(SEPARATOR) || at(ATTRIBUTE_REF_START) || at(ATTR_LIST_SEP) || at(ATTR_LIST_OP)
      || at(SINGLE_QUOTE) || at(DOUBLE_QUOTE) || at(ASSIGNMENT) || at(URL_LINK) || at(BLOCKID)) {
      if (at(ATTRS_END)) {
        next();
        break;
      } else if (at(ATTR_NAME)) {
        parseAttributeInBrackets(null);
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else if (at(URL_LINK)) {
        parseUrl();
      } else if (at(BLOCKID)) {
        parseId();
      } else {
        next();
      }
    }
    blockAttrsMarker.done(AsciiDocElementTypes.BLOCK_ATTRIBUTES);
  }

  private void parseInlineMacro() {
    newLines = 0;
    PsiBuilder.Marker inlineMacroMarker = myBuilder.mark();
    String macroId = myBuilder.getTokenText();
    next();
    while (atInlineMacro()
      && newLines == 0) {
      if (at(INLINE_ATTRS_END)) {
        next();
        break;
      } else if (at(URL_LINK)) {
        parseUrl();
      } else if (at(PASSTRHOUGH_INLINE_START)) {
        parsePassthrough();
      } else if (at(ATTR_NAME)) {
        parseAttributeInBrackets(macroId);
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
    inlineMacroMarker.done(AsciiDocElementTypes.INLINE_MACRO);
  }

  private boolean atInlineMacro() {
    return at(INLINE_MACRO_BODY) || at(ATTR_NAME) || at(ASSIGNMENT) || at(URL_LINK) || at(ATTR_VALUE) || at(SEPARATOR) || at(INLINE_ATTRS_START) || at(INLINE_ATTRS_END) || at(MACROTEXT)
      || at(DOUBLE_QUOTE) || at(SINGLE_QUOTE) || at(ATTRIBUTE_REF_START) || at(ATTR_LIST_SEP) || at(ATTR_LIST_OP) || at(PASSTRHOUGH_INLINE_START);
  }

  private void parsePassthrough() {
    while (at(PASSTRHOUGH_INLINE_START)) {
      next();
    }
    while (at(PASSTRHOUGH_CONTENT) || at(PASSTRHOUGH_INLINE_END)) {
      if (at(PASSTRHOUGH_INLINE_END)) {
        next();
        break;
      }
      next();
    }
  }

  private void parseAttributeInBrackets(String macroId) {
    if (emptyLines > 0) {
      // avoid adding empty markers if empty lines already present
      next();
      return;
    }
    PsiBuilder.Marker attributeInBracketMarker = myBuilder.mark();
    String name = null;
    while ((at(ATTR_NAME) || at(ASSIGNMENT) || at(URL_LINK) || at(ATTR_VALUE)
      || at(DOUBLE_QUOTE) || at(SINGLE_QUOTE) || at(ATTRIBUTE_REF_START) || at(ATTR_LIST_SEP) || at(ATTR_LIST_OP) || at(CONTINUATION))
      && emptyLines == 0) {
      if (at(URL_LINK)) {
        parseUrl();
      } else if (at(ATTR_NAME)) {
        name = myBuilder.getTokenText();
        next();
      } else if (at(ATTR_VALUE) && ("tags".equals(name) || "tag".equals(name)) && "include::".equals(macroId)) {
        PsiBuilder.Marker tag = myBuilder.mark();
        next();
        tag.done(AsciiDocElementTypes.INCLUDE_TAG);
      } else if ((at(ATTRIBUTE_REF_START) || at(ATTR_VALUE)) && "id".equals(name) && macroId == null) {
        PsiBuilder.Marker blockIdMarker = myBuilder.mark();
        while ((at(ATTRIBUTE_REF_START) || at(ATTR_VALUE)) && emptyLines == 0) {
          if (at(ATTRIBUTE_REF_START)) {
            parseAttributeReference();
          } else {
            next();
          }
        }
        blockIdMarker.done(AsciiDocElementTypes.BLOCKID);
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
    attributeInBracketMarker.done(AsciiDocElementTypes.ATTRIBUTE_IN_BRACKETS);
  }

  private void parseTitle() {
    newLines = 0;
    markPreBlock();
    PsiBuilder.Marker titleMarker = myBuilder.mark();
    while (!myBuilder.eof() && newLines == 0) {
      if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else if (at(URL_START) || at(URL_LINK) || at(URL_EMAIL) || at(URL_PREFIX)) {
        parseUrl();
      } else if (at(INLINE_MACRO_ID)) {
        parseInlineMacro();
      } else if (at(REFSTART)) {
        parseRef();
      } else {
        next();
      }
    }
    titleMarker.done(AsciiDocElementTypes.TITLE);
  }

  private void parseAttributeReference() {
    PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
    next();
    while (at(ATTRIBUTE_REF) || at(ATTRIBUTE_REF_END)) {
      if (at(ATTRIBUTE_REF_END)) {
        next();
        break;
      }
      next();
    }
    blockAttrsMarker.done(AsciiDocElementTypes.ATTRIBUTE_REF);
  }

  private void parseAttributeDeclaration() {
    newLines = 0;
    PsiBuilder.Marker blockAttrsMarker = myBuilder.mark();
    next();
    while ((at(ATTRIBUTE_NAME) || at(ATTRIBUTE_NAME_END) || at(ATTRIBUTE_VAL) || at(ATTRIBUTE_REF_START)
      || at(ATTRIBUTE_CONTINUATION) || at(ATTRIBUTE_CONTINUATION_LEGACY) || at(ATTRIBUTE_UNSET) || at(ATTRIBUTE_SOFTSET))
      && newLines == 0) {
      if (at(ATTRIBUTE_NAME)) {
        PsiBuilder.Marker blockIdMarker = myBuilder.mark();
        next();
        blockIdMarker.done(AsciiDocElementTypes.ATTRIBUTE_DECLARATION_NAME);
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else {
        next();
      }
    }
    blockAttrsMarker.done(AsciiDocElementTypes.ATTRIBUTE_DECLARATION);
  }

  private void parseBlockMacro() {
    newLines = 0;
    markPreBlock();
    PsiBuilder.Marker myBlock = myPreBlockMarker;
    myPreBlockMarker = null;
    String macroId = myBuilder.getTokenText();
    next();
    while (at(ATTRIBUTE_REF) || at(SEPARATOR)) {
      if (at(ATTRIBUTE_REF)) {
        parseAttributeReference();
        continue;
      }
      next();
    }
    while ((at(BLOCK_MACRO_BODY) || at(ATTRIBUTE_REF_START) || at(ATTR_NAME) || at(ATTR_VALUE) || at(SEPARATOR) || at(ATTRS_START) || at(ATTRS_END)
      || at(ASSIGNMENT) || at(SINGLE_QUOTE) || at(DOUBLE_QUOTE) || at(ATTRIBUTE_REF) || at(BLOCK_MACRO_ID) || at(ATTR_LIST_SEP) || at(ATTR_LIST_OP)
      || at(URL_START) || at(URL_LINK) || at(URL_EMAIL) || at(URL_PREFIX) || at(INLINE_MACRO_ID) || at(ATTRIBUTE_NAME_START) || at(TEXT)
      || at(LBRACKET) || at(RBRACKET) || at(LT) || at(GT) || at(LPAREN) || at(RPAREN)
      || ALLQUOTES.contains(myBuilder.getTokenType()))
      && newLines == 0) {
      if (at(ATTRS_END)) {
        next();
        break;
      } else if (QUOTEPAIRS.containsKey(myBuilder.getTokenType())) {
        parseQuoted(new HashSet<>());
      } else if (at(ATTR_NAME)) {
        parseAttributeInBrackets(macroId);
      } else if (at(URL_START) || at(URL_LINK) || at(URL_EMAIL) || at(URL_PREFIX)) {
        parseUrl();
      } else if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
      } else if (at(BLOCK_MACRO_ID)) {
        parseBlockMacro();
      } else if (at(INLINE_MACRO_ID)) {
        parseInlineMacro();
      } else if (at(ATTRIBUTE_NAME_START)) {
        parseAttributeDeclaration();
      } else {
        next();
      }
    }
    myBlock.done(AsciiDocElementTypes.BLOCK_MACRO);
  }

  private void parseUrl() {
    newLines = 0;
    PsiBuilder.Marker inlineMacroMarker = myBuilder.mark();
    // avoid combining two links or two emails
    boolean seenLinkOrEmail = false;
    while ((at(URL_START) || at(URL_LINK) || at(URL_EMAIL) || at(URL_PREFIX) || at(ATTR_NAME) || at(SEPARATOR) || at(INLINE_ATTRS_START) || at(MACROTEXT)
      || at(URL_END) || at(INLINE_ATTRS_END) || at(ATTRIBUTE_REF_START))
      && newLines == 0) {
      if (at(ATTR_NAME)) {
        parseAttributeInBrackets(null);
        continue;
      }
      if (at(ATTRIBUTE_REF_START)) {
        parseAttributeReference();
        continue;
      }
      if (at(URL_LINK) || at(URL_EMAIL)) {
        if (!seenLinkOrEmail) {
          seenLinkOrEmail = true;
          next();
          continue;
        } else {
          break;
        }
      }
      if (at(URL_END) || at(INLINE_ATTRS_END)) {
        next();
        break;
      }
      next();
    }
    inlineMacroMarker.done(AsciiDocElementTypes.URL);
  }

  private void closeBlocks() {
    while (!myBlockMarker.isEmpty()) {
      closeBlockMarker();
    }
  }

  private void closeBlockMarker() {
    BlockMarker currentBLock = myBlockMarker.pop();
    currentBLock.marker.done(currentBLock.type);
  }

  private void parseBlock() {
    String delimiter = myBuilder.getTokenText();
    if (delimiter == null) {
      return;
    }
    delimiter = delimiter.trim();
    if (myBuilder.getTokenType() == CELLSEPARATOR) {
      // the last character of the cell separator defines the separator, for example the "|"
      delimiter = delimiter.substring(delimiter.length() - 1);
    }
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
        closeBlockMarker();
      }
      if (myBuilder.getTokenType() != CELLSEPARATOR) {
        next();
      }
      // close this block
      closeBlockMarker();
    }
    if (!existsInStack || myBuilder.getTokenType() == CELLSEPARATOR) {
      PsiBuilder.Marker myBlockStartMarker;
      if (myPreBlockMarker != null) {
        myBlockStartMarker = myPreBlockMarker;
        myPreBlockMarker = null;
      } else {
        myBlockStartMarker = beginBlock();
      }
      IElementType type;
      if (myBuilder.getTokenType() == CELLSEPARATOR) {
        type = AsciiDocElementTypes.CELL;
      } else {
        type = AsciiDocElementTypes.BLOCK;
      }
      myBlockMarker.push(new BlockMarker(delimiter, myBlockStartMarker, type));
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
    myBlockMarker.push(new BlockMarker("nodel", myBlockStartMarker, AsciiDocElementTypes.BLOCK));
  }

  private void endBlockNoDelimiter() {
    if (myBlockMarker.size() > 0 && myBlockMarker.peek().delimiter.equals("nodel")) {
      dropPreBlock();
      closeBlockMarker();
    }
  }

  private void startEnumerationDelimiter() {
    String sign;
    Objects.requireNonNull(myBuilder.getTokenText());
    IElementType type = LIST_ITEM;
    // retrieve the enumeration sign type, to allow closing enumerations of the same type
    if (at(ENUMERATION)) {
      sign = myBuilder.getTokenText().replaceAll("^([0-9]+|[a-zA-Z]?)", "");
    } else if (at(DESCRIPTION_END)) {
      type = DESCRIPTION_ITEM;
      sign = myBuilder.getTokenText();
      if (myPreBlockMarker == null) {
        return;
      }
    } else if (at(CALLOUT)) {
      sign = "callout";
    } else {
      sign = myBuilder.getTokenText();
    }
    boolean otherItemExisted = false;
    while (myBlockMarker.stream().anyMatch(o -> o.delimiter.equals("enum_" + sign)) &&
      myBlockMarker.peek().delimiter.startsWith("enum")) {
      if (myBlockMarker.peek().delimiter.equals("enum_" + sign)) {
        otherItemExisted = true;
      }
      endEnumerationDelimiter();
    }
    PsiBuilder.Marker myBlockStartMarker;
    if (myPreBlockMarker != null) {
      myBlockStartMarker = myPreBlockMarker;
      myPreBlockMarker = null;
    } else {
      myBlockStartMarker = beginBlock();
    }
    if (!otherItemExisted) {
      myBlockMarker.push(new BlockMarker("list_" + sign, myBlockStartMarker.precede(), LIST));
      if (type == LIST_ITEM) {
        myBlockStartMarker.drop();
        myBlockStartMarker = beginBlock();
      }
    }
    myBlockMarker.push(new BlockMarker("enum_" + sign, myBlockStartMarker, type));
  }

  private void endEnumerationDelimiter() {
    if (myBlockMarker.size() > 0 && myBlockMarker.peek().delimiter.startsWith("enum")) {
      if (myPreBlockMarker != null) {
        closeBlockMarker(myPreBlockMarker);
      } else {
        closeBlockMarker();
      }
    }
  }

  private void endListDelimiter() {
    endEnumerationDelimiter();
    if (myBlockMarker.size() > 0 && myBlockMarker.peek().delimiter.startsWith("list")) {
      if (myPreBlockMarker != null) {
        closeBlockMarker(myPreBlockMarker);
      } else {
        closeBlockMarker();
      }
    }
  }

  private void parseBlockElement(IElementType elementType) {
    PsiBuilder.Marker myBlockStartMarker;
    if (myPreBlockMarker != null) {
      myBlockStartMarker = myPreBlockMarker;
      myPreBlockMarker = null;
    } else {
      myBlockStartMarker = beginBlock();
    }
    String marker = myBuilder.getTokenText();
    Objects.requireNonNull(marker);
    marker = marker.trim();
    IElementType type = myBuilder.getTokenType();
    next();
    while (true) {
      if (myBuilder.eof()) {
        break;
      }
      // the block needs to be terminated by the same sequence that started it
      if (at(type) && myBuilder.getTokenText() != null && myBuilder.getTokenText().trim().equals(marker)) {
        next();
        break;
      }
      if (at(BLOCK_MACRO_ID)) {
        parseBlockMacro();
      } else {
        next();
      }
    }
    myBlockStartMarker.done(elementType);
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
        parseBlockMacro();
      } else {
        newLines = 0;
        next();
        // eof() triggers skipWhitespace that increments newLines
        if (!myBuilder.eof() && newLines > 1) {
          break;
        }
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

  @SuppressWarnings("FallThrough")
  public static int headingLevel(@Nullable CharSequence headingText) {
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

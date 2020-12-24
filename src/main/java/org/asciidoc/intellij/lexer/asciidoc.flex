package org.asciidoc.intellij.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.psi.AsciiDocInlineMacro;
import java.util.Stack;

/*
To find out more about lexing, have a look at the contributor's guide for coders at:
https://intellij-asciidoc-plugin.ahus1.de/docs/contributors-guide/coder/lexing-and-parsing.html
*/

%%

%class _AsciiDocLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

%{
  private static final char[] NUMBERS = "1234567890".toCharArray();
  private static final char[] SPACES = " \t".toCharArray();
  private static final char[] COLONSLASH = ":/".toCharArray();
  private int blockDelimiterLength;
  private boolean singlebold = false;
  private boolean doublebold = false;
  private boolean singleitalic = false;
  private boolean doubleitalic = false;
  private boolean singlemono = false;
  private boolean doublemono = false;
  private boolean typographicquote = false;
  private boolean isTags = false;
  private String style = null;
  private int headerLines = 0;
  private char tableChar = 0;

  private Stack<Integer> stateStack = new Stack<>();

  private Stack<String> blockStack = new Stack<>();

  private int zzEndReadFinal;
  private int zzCachedEndRead;

  public void setFinal(int endFinal) {
    zzEndReadFinal = endFinal;
    zzCachedEndRead = 0;
  }

  public int clearLookahead() {
    zzEndRead = zzEndReadFinal;
    return zzEndRead;
  }

  public int limitLookahead() {
    return limitLookahead(zzCurrentPos);
  }

  public int limitLookahead(int zzCurrentPosL) {
    if (zzEndReadFinal > zzCurrentPosL + 2000) {
      zzEndRead = zzCurrentPosL + 2000;
    } else {
      zzEndRead = zzEndReadFinal;
    }
    if (zzCurrentPosL < zzEndReadFinal && tableChar != 0) {
      if (zzCachedEndRead >= zzCurrentPosL +3 && zzBuffer.charAt(zzCachedEndRead -2) == tableChar && zzBuffer.charAt(zzCachedEndRead -2 -1) != '\\' && zzBuffer.charAt(zzCachedEndRead -2 +1) != '=') {
        zzEndRead = zzCachedEndRead;
      } else {
        int nextstop = zzCurrentPosL + 1;
        while (nextstop < zzEndRead -2) {
          if (zzBuffer.charAt(nextstop) == tableChar && zzBuffer.charAt(nextstop) -1 != '\\' && zzBuffer.charAt(nextstop+1) != '=') {
            zzCachedEndRead = zzEndRead = nextstop +2;
            break;
          }
          ++ nextstop;
        }
      }
    }
    return zzEndRead;
  }

  private boolean isPrefixedBy(char[] prefix) {
    if(getTokenStart() > 0) {
      char c = zzBuffer.charAt(getTokenStart() -1);
      for (char p : prefix) {
        if (c == p) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isUnconstrainedEnd() {
    if(getTokenStart() > 0) {
      char c = zzBuffer.charAt(getTokenStart() -1);
      if (c == ' ' || c == '\t' || c == '\n') {
        return false;
      }
    }
    if(getTokenEnd() < zzBuffer.length()) {
      char c = zzBuffer.charAt(getTokenEnd());
      if (Character.isAlphabetic(c) || Character.isDigit(c) || c == '_') {
        return false;
      }
    }
    return true;
  }

  private boolean isUnconstrainedStart() {
    if(getTokenStart() > 0) {
      char c = zzBuffer.charAt(getTokenStart() -1);
      if (Character.isAlphabetic(c) || Character.isDigit(c) || c == '_' || c == ':' || c == ';' || c == '\\') {
        return false;
      }
    }
    if(getTokenEnd() < zzBuffer.length()) {
      char c = zzBuffer.charAt(getTokenEnd());
      if (Character.isSpaceChar(c)) {
        return false;
      }
    }
    return true;
  }

  private boolean isEscaped() {
    if(getTokenStart() > 0) {
      char c = zzBuffer.charAt(getTokenStart() -1);
      if (c == '\\') {
        return true;
      }
    }
    return false;
  }

  private boolean isNoDel() {
    if(blockStack.size() > 0 && blockStack.peek().startsWith("nodel")) {
      return true;
    }
    return false;
  }

  private boolean isNoDelList() {
    if(blockStack.size() > 0 && blockStack.peek().startsWith("nodel-list")) {
      return true;
    }
    return false;
  }

  private boolean isNoDelVerse() {
    if(blockStack.size() > 0 && blockStack.peek().startsWith("nodel-verse")) {
      return true;
    }
    return false;
  }

  private void resetFormatting() {
    singlebold = false;
    doublebold = false;
    singleitalic = false;
    doubleitalic = false;
    singlemono = false;
    doublemono = false;
    typographicquote = false;
    isTags = false;
  }
  private IElementType textFormat() {
    if(yystate() == DESCRIPTION) {
      return AsciiDocTokenTypes.DESCRIPTION;
    }
    if(yystate() == ATTRIBUTE_VAL) {
      return AsciiDocTokenTypes.ATTRIBUTE_VAL;
    }
    if(yystate() == LINKFILE || yystate() == LINKFILEWITHBLANK) {
      return AsciiDocTokenTypes.LINKFILE;
    }
    if(yystate() == BLOCK_MACRO) {
      return AsciiDocTokenTypes.BLOCK_MACRO_BODY;
    }
    if(yystate() == LINKTEXT) {
      return AsciiDocTokenTypes.LINKTEXT;
    }
    if(yystate() == REFTEXT) {
      return AsciiDocTokenTypes.REFTEXT;
    }
    if(yystate() == BLOCKREFTEXT) {
      return AsciiDocTokenTypes.BLOCKREFTEXT;
    }
    if(yystate() == KBD_MACRO_ATTRS) {
      return AsciiDocTokenTypes.INLINE_MACRO_BODY;
    }
    if((doublemono || singlemono) && (singlebold || doublebold) && (doubleitalic || singleitalic)) {
      return AsciiDocTokenTypes.MONOBOLDITALIC;
    } else if((doublemono || singlemono) && (singlebold || doublebold)) {
      return AsciiDocTokenTypes.MONOBOLD;
    } else if((doublemono || singlemono) && (singleitalic || doubleitalic)) {
      return AsciiDocTokenTypes.MONOITALIC;
    } else if(doublemono || singlemono) {
      return AsciiDocTokenTypes.MONO;
    } else if((singlebold || doublebold) && (singleitalic || doubleitalic)) {
      return AsciiDocTokenTypes.BOLDITALIC;
    } else if(singleitalic || doubleitalic) {
      return AsciiDocTokenTypes.ITALIC;
    } else if(singlebold || doublebold) {
      return AsciiDocTokenTypes.BOLD;
    } else {
      return AsciiDocTokenTypes.TEXT;
    }
  }
  private void clearStyle() {
    style = null;
  }
  private void setStyle(String style) {
    if (this.style == null) {
      this.style = style;
    }
  }

  private void yypushstate () {
    stateStack.push(yystate());
  }

  private void yypopstate () {
    if(stateStack.size() > 0) {
      yybegin(stateStack.pop());
    } else {
      yybegin(YYINITIAL);
    }
  }

  private boolean isInAttribute () {
    if(stateStack.contains(ATTR_PARSEABLE)) {
      return true;
    } else {
      return false;
    }
  }

  private void yyinitialIfNotInBlock() {
    if (blockStack.size() == 0 && style == null) {
      yybegin(YYINITIAL);
    } else {
      yybegin(PREBLOCK);
    }
  }
%}

SPACE = [\ \t]
NON_SPACE = [^\n]
LINE_COMMENT="//"[^\n]*
COMMENT_BLOCK_DELIMITER = "////" "/"* {SPACE}*
PASSTRHOUGH_BLOCK_DELIMITER = "++++" "+"* {SPACE}*
LISTING_BLOCK_DELIMITER = "----" "-"* {SPACE}*
// a listing might be terminated by an open block if this is what started it
LISTING_BLOCK_DELIMITER_END = "--" "-"* {SPACE}*
MARKDOWN_LISTING_BLOCK_DELIMITER = "```" {SPACE}*
EXAMPLE_BLOCK_DELIMITER = "====" "="* {SPACE}*
SIDEBAR_BLOCK_DELIMITER = "****" "*"* {SPACE}*
QUOTE_BLOCK_DELIMITER = "____" "_"* {SPACE}*
LITERAL_BLOCK_DELIMITER = "...." "."* {SPACE}*
TABLE_BLOCK_DELIMITER = [|!] "===" "="* {SPACE}*
CELLPREFIX = ([0-9][+*])?[\^<>]?[\.]?([0-9][+])?[\^<>]?[aehlmdsv]?
OPEN_BLOCK_DELIMITER = "--" {SPACE}*
CONTINUATION = "+"
HEADING_START = "="{1,6} {SPACE}+
HEADING_START_MARKDOWN = "#"{1,6} {SPACE}+
// starting at the start of the line, but not with a dot
// next line following with only header marks
HEADING_OLDSTYLE = [^ .\n\t].* "\n" [-=~\^+]+ {SPACE}* "\n"
IFDEF_IFNDEF = ("ifdef"|"ifndef"|"endif") "::"
BLOCK_MACRO_START = [a-zA-Z0-9_]+"::"
INLINE_MACRO_START = [a-zA-Z0-9_]+":"
TITLE_START = "."
AUTOCOMPLETE = "IntellijIdeaRulezzz" " "? // CompletionUtilCore.DUMMY_IDENTIFIER - blank might get missing at end of line, therefore keep it optional
BLOCK_ATTRS_START = "["
STRING = {NON_SPACE}+ \n? // something that doesn't have an empty line
STRINGNOASTERISK   = [^\n\*]+ \n?
STRINGNOUNDERSCORE = [^\n\_]+ \n?
STRINGNOBACKTICK   = [^\n\`]+ \n?
STRINGNOPLUS       = [^\n\+]+ \n?
// something with a non-blank at the end, might contain a line break, but only if it doesn't separate the block
WORD = {SPACE}* [^\n]* {SPACE}* \n {SPACE}* [^\ \t\n] | {SPACE}* [^\n]*[^\ \t\n]
WORDNOBRACKET =  {SPACE}* [^\n\]]* {SPACE}* \n {SPACE}* [^\ \t\n\]] | {SPACE}* [^\n\]]*[^\ \t\n\]]
WORDNOASTERISK = {SPACE}* [^\n\*]* {SPACE}* \n {SPACE}* [^\ \t\n\*] | {SPACE}* [^\n\*]*[^\ \t\n\*]
WORDNOUNDERSCORE={SPACE}* [^\n\_]* {SPACE}* \n {SPACE}* [^\ \t\n\_] | {SPACE}* [^\n\_]*[^\ \t\n\_]
WORDNOBACKTICK = {SPACE}* [^\n\`]* {SPACE}* \n {SPACE}* [^\ \t\n\`] | {SPACE}* [^\n\`]*[^\ \t\n\`]
WORDNOPLUS =     {SPACE}* [^\n\+]* {SPACE}* \n {SPACE}* [^\ \t\n\+] | {SPACE}* [^\n\+]*[^\ \t\n\+]
BOLD = "*"
BULLET = ("*"+|"-")
ENUMERATION = ([0-9]*|[a-zA-Z]?)"."+
CALLOUT = "<" ([0-9]+|".") ">"
DOUBLEBOLD = {BOLD} {BOLD}
PASSTRHOUGH_INLINE = "+++"
ITALIC = "_"
DOUBLEITALIC = {ITALIC} {ITALIC}
MONO = "`"
DOUBLEMONO = {MONO} {MONO}
LPAREN = "("
RPAREN = ")"
LBRACKET = "["
RBRACKET = "]"
LT = "<"
GT = ">"
REFSTART = "<<"
REFEND = ">>"
PAGEBREAK = "<<<" "<"* {SPACE}*
HORIZONTALRULE = ("-" {SPACE}* "-" {SPACE}* "-" {SPACE}*) |  ("*" {SPACE}* "*" {SPACE}* "*" {SPACE}*) |  ("_" {SPACE}* "_" {SPACE}* "_" {SPACE}*) | "'''" "'"*
BLOCKIDSTART = "[["
BLOCKIDEND = "]]"
INLINEIDSTART = "[["
INLINEIDEND = "]]"
SINGLE_QUOTE = "'"
DOUBLE_QUOTE = "\""
TYPOGRAPHIC_DOUBLE_QUOTE_START = "\"`"
TYPOGRAPHIC_DOUBLE_QUOTE_END = "`\""
TYPOGRAPHIC_SINGLE_QUOTE_START = "'`"
TYPOGRAPHIC_SINGLE_QUOTE_END = "`'"
ANCHORSTART = "[#"
ANCHOREND = "]"
BIBSTART = "[[["
BIBEND = "]]]"
LINKSTART = "link:"
XREFSTART = "xref:"
LINKTEXT_START = "["
INLINE_URL_NO_DELIMITER = (https?|file|ftp|irc): "//" [^\n\s\[\]<]*([^\n\s.,;\[\]<\)\'\"])
INLINE_URL_WITH_DELIMITER = (https?|file|ftp|irc): "//" [^\n\s\[\]<]*([^\n\s\[\]\'\"])
INLINE_EMAIL_NO_DELIMITER = [[:letter:][:digit:]_](&amp;|[[:letter:][:digit:]_\-.%+])*@[[:letter:][:digit:]][[:letter:][:digit:]_\-.]*\.[a-zA-Z]{2,5}
LINKEND = "]"
ATTRIBUTE_NAME_START = ":"
ATTRIBUTE_NAME_DECL = [a-zA-Z0-9_]+ [a-zA-Z0-9_ \t-]*
ATTRIBUTE_NAME = [a-zA-Z0-9_]+ [a-zA-Z0-9_-]*
ATTRIBUTE_NAME_END = ":"
ATTRIBUTE_REF_START = "{"
ATTRIBUTE_REF_END = "}"
END_OF_SENTENCE = [\.?!] | (" " [?!]) // French: "marks with two elements require a space before them in"
HARD_BREAK = {SPACE} "+" {SPACE}* "\n"
DESCRIPTION_END = (":"{2,4} | ";;")
DESCRIPTION = [^\n]+ {SPACE}* {DESCRIPTION_END}
ADMONITION = ("NOTE" | "TIP" | "IMPORTANT" | "CAUTION" | "WARNING" ) ":"

%state MULTILINE
%state FRONTMATTER
%state HEADER
%state INSIDE_HEADER_LINE
%state EOL_POP
%state PREBLOCK
%state LINECOMMENT
%state STARTBLOCK
%state DELIMITER
%state SINGLELINE
%state DESCRIPTION
%state LIST
%state INSIDE_LINE
%state PASSTHROUGH_SECOND_TRY
%state MONO_SECOND_TRY
%state REF
%state REFTEXT
%state REFAUTO
%state BLOCKID
%state BLOCKREFTEXT
%state INLINEID
%state INLINEREFTEXT
%state HEADING
%state DOCTITLE
%state ANCHORID
%state ANCHORREFTEXT
%state BIBSTART
%state BIBID
%state BIBNAME

%state INLINE_URL_NO_DELIMITER
%state INLINE_URL_IN_BRACKETS
%state INLINE_EMAIL_WITH_PREFIX

%state LISTING_BLOCK
%state LISTING_NO_DELIMITER

%state COMMENT_BLOCK
%state LITERAL_BLOCK
%state PASSTRHOUGH_BLOCK

%state PASSTRHOUGH_INLINE
%state PASSTHROUGH_NO_DELIMITER
%state PASSTHROUGH_NO_DELIMITER
%state PASSTRHOUGH_INLINE_CONSTRAINED
%state PASSTRHOUGH_INLINE_UNCONSTRAINED

%state IFDEF_IFNDEF_ENDIF
%state ATTR_PARSEABLE
%state BLOCK_MACRO
%state BLOCK_MACRO_ATTRS
%state ATTRS_SINGLE_QUOTE
%state ATTRS_DOUBLE_QUOTE
%state ATTRS_NO_QUOTE
%state ATTRS_SINGLE_QUOTE_START
%state ATTRS_DOUBLE_QUOTE_START
%state ATTRS_SINGLE_QUOTE_START_NO_CLOSE
%state ATTRS_DOUBLE_QUOTE_START_NO_CLOSE
%state ATTR_VAL_START
%state BLOCK_ATTRS

%state INLINE_MACRO
%state INLINE_MACRO_URL
%state KBD_MACRO
%state INLINE_MACRO_ATTRS
%state KBD_MACRO_ATTRS
%state KBD_MACRO_ATTRS_KEY

%state TITLE

%state ATTRIBUTE_NAME
%state ATTRIBUTE_VAL
%state ATTRIBUTE_VAL_WS
%state ATTRIBUTE_REF

%state LINKFILE
%state LINKFILEWITHBLANK
%state LINKURL
%state LINKANCHOR
%state LINKTEXT

%%

// IntelliJ might do partial parsing from any YYINITIAL inside a document
// therefore only return here is no other state (i.e. bold) needs to be preserved
<YYINITIAL> {
  [^]                  {
        if (isPrefixedBy("|".toCharArray())) {
          tableChar = '|';
          zzEndReadL = limitLookahead(zzCurrentPosL);
        } else {
          tableChar = 0;
          zzEndReadL = limitLookahead(zzCurrentPosL);
        }
        yypushback(yylength()); clearStyle(); resetFormatting(); blockStack.clear(); stateStack.clear(); yybegin(MULTILINE);
      }
}

<MULTILINE> {
  "---" \n [a-zA-Z0-9_]+ ":" {
        if (zzMarkedPos == yylength()) {
          yybegin(FRONTMATTER); yypushstate(); yypushback(yylength()-3); yybegin(EOL_POP);
          return AsciiDocTokenTypes.FRONTMATTER_DELIMITER;
        } else {
          yypushback(yylength()); yybegin(PREBLOCK);
        }
      }

  {HEADING_OLDSTYLE} {
        if (blockStack.size() > 0) {
          // headings must not be nested in block
          yypushback(yylength());
          yybegin(PREBLOCK);
        } else {
          String[] part = yytext().toString().split("\n");
          // remove all trailing white space
          String heading = part[0].replaceAll("[ \t]*$","");
          String underlining = part[1].replaceAll("[ \t]*$","");
          boolean sameCharactersInSecondLine = true;
          // must be same character all of second line
          for(int i = 0; i < underlining.length(); ++i) {
            if(underlining.charAt(0) != underlining.charAt(i)) {
              sameCharactersInSecondLine = false;
              break;
            }
          }
          // must be same length plus/minus one character
          if(heading.length() >= underlining.length() -1
             && heading.length() <= underlining.length() +1
             && sameCharactersInSecondLine
             // no line (or block) comments please
             && !heading.matches("//.*")
             // needs to contain alphanumeric character - see SetextSectionTitleRx
             && heading.matches(".*\\p{Alnum}.*")
             // if it is in brackets, it is a block type
             && !heading.matches("\\[.*\\]")) {
            // push back the second newline of the pattern

            yypushback(1);
            resetFormatting();
            if (underlining.startsWith("=") && style == null) {
              headerLines = 0;
              yybegin(HEADER);
            }
            return AsciiDocTokenTypes.HEADING_OLDSTYLE;
          } else {
            // pass this contents to the single line rules (second priority)
            yypushback(yylength());
            yybegin(PREBLOCK);
          }
        }
      }
  [^] {
        yypushback(yylength()); yybegin(PREBLOCK);
      }
}

<ATTRIBUTE_NAME> {
  {ATTRIBUTE_NAME_END} { yybegin(ATTRIBUTE_VAL_WS); return AsciiDocTokenTypes.ATTRIBUTE_NAME_END; }
  {AUTOCOMPLETE} | {ATTRIBUTE_NAME_DECL} { return AsciiDocTokenTypes.ATTRIBUTE_NAME; }
  "!"                { return AsciiDocTokenTypes.ATTRIBUTE_UNSET; } // can be at start or end of declaration
  "\n"               { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                { yypushback(yylength()); yypopstate(); }
}

<ATTRIBUTE_VAL_WS> {
  " " { return AsciiDocTokenTypes.WHITE_SPACE; }
  [^]                { yypushback(yylength());  yybegin(ATTRIBUTE_VAL); }
}

<ATTRIBUTE_VAL, INLINE_MACRO, INLINE_URL_NO_DELIMITER, INSIDE_LINE, DOCTITLE, HEADING, DESCRIPTION, LINKFILE, LINKFILEWITHBLANK, LINKANCHOR, LINKURL, BLOCK_MACRO, LINKTEXT, REFTEXT, INLINEREFTEXT, BLOCKREFTEXT, BLOCK_MACRO_ATTRS, ATTRS_SINGLE_QUOTE, ATTRS_DOUBLE_QUOTE, ATTRS_NO_QUOTE, TITLE, REF, ANCHORID, BIBNAME, BLOCK_ATTRS> {
  {ATTRIBUTE_REF_START} ( {ATTRIBUTE_NAME}? {ATTRIBUTE_REF_END} | [^}\n ]* {AUTOCOMPLETE} ) {
                         yypushback(yylength() - 1);
                         if (!isEscaped()) {
                           yypushstate(); yybegin(ATTRIBUTE_REF); return AsciiDocTokenTypes.ATTRIBUTE_REF_START;
                         } else {
                           textFormat();
                         }
                       }
  // parse the start of a REF to allow brace-matcher to autocomplete this
  {ATTRIBUTE_REF_START} {
                         if (!isEscaped()) {
                           return AsciiDocTokenTypes.ATTRIBUTE_REF_START;
                         } else {
                           textFormat();
                         }
                       }
}

<ATTRIBUTE_VAL> {
  /*Value continue on the next line if the line is ended by a space followed by a backslash*/
  {SPACE} "\\" {SPACE}* "\n" {SPACE}* { return AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION; }
  /*Value continue on the next line if the line is ended by a space followed by a plus, but this is old syntax*/
  {SPACE} {CONTINUATION} {SPACE}* "\n" {SPACE}* {
                         return AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION_LEGACY;
                       }
  {RBRACKET}           { if (isInAttribute()) {
                           yypushback(1); yypopstate(); return AsciiDocTokenTypes.ATTRIBUTE_VAL;
                         } else {
                           return AsciiDocTokenTypes.ATTRIBUTE_VAL;
                         }
                       }
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.ATTRIBUTE_VAL; }
}

<LIST, PREBLOCK> {
  // bibtext doesn't allow for line breaks. Adding {STRING} at the end so that it is as long as the regular bullet match
  {BULLET} / {SPACE}+ {BIBSTART} [^\n]* {BIBEND} ({STRING} | \n) {
        String delimiter = "nodel-list-bullet-" + yytext();
        while (blockStack.contains(delimiter)) {
          blockStack.pop();
        }
        blockStack.push(delimiter);
        resetFormatting();
        yybegin(BIBSTART);
        return AsciiDocTokenTypes.BULLET;
  }
  {BULLET} / {SPACE}+ {STRING} {
        String delimiter = "nodel-list-bullet-" + yytext();
        while (blockStack.contains(delimiter)) {
          blockStack.pop();
        }
        blockStack.push(delimiter);
        resetFormatting(); yybegin(INSIDE_LINE); return AsciiDocTokenTypes.BULLET;
      }
  {ENUMERATION} / {SPACE}+ {STRING} {
        String delimiter = "nodel-list-enum-" + yytext();
        while (blockStack.contains(delimiter)) {
          blockStack.pop();
        }
        blockStack.push(delimiter);
        resetFormatting(); yybegin(INSIDE_LINE); return AsciiDocTokenTypes.ENUMERATION;
      }
  {DESCRIPTION} / {SPACE}+ {STRING} {
        String delimiter = "nodel-list-desc-";
        int end = getTokenEnd();
        char c = zzBuffer.charAt(end-1);
        int start = getTokenEnd() - 1;
        while (zzBuffer.charAt(start) == c) {
          -- start;
        }
        delimiter += zzBuffer.subSequence(start+1, end);
        while (blockStack.contains(delimiter)) {
          blockStack.pop();
        }
        blockStack.push(delimiter);
        resetFormatting(); yybegin(INSIDE_LINE); yypushstate(); yybegin(DESCRIPTION); yypushback(yylength());
      }
  {DESCRIPTION} / {SPACE}* "\n" {
        String delimiter = "nodel-list-desc-";
        int end = getTokenEnd();
        char c = zzBuffer.charAt(end-1);
        int start = getTokenEnd() - 1;
        while (zzBuffer.charAt(start) == c) {
          -- start;
        }
        delimiter += zzBuffer.subSequence(start+1, end);
        while (blockStack.contains(delimiter)) {
          blockStack.pop();
        }
        blockStack.push(delimiter);
        resetFormatting(); yybegin(INSIDE_LINE); yypushstate(); yybegin(DESCRIPTION); yypushback(yylength());
      }
  ^ {CALLOUT} / {SPACE}+ {STRING} {
        String delimiter = "nodel-list-callout";
        while (blockStack.contains(delimiter)) {
          blockStack.pop();
        }
        blockStack.push(delimiter);
        resetFormatting(); yybegin(INSIDE_LINE); return AsciiDocTokenTypes.CALLOUT;
      }
}

<LIST> {
  ^ {CONTINUATION} {SPACE}* "\n" {
                         yypushback(yylength() - 1);
                         yybegin(PREBLOCK);
                         yypushstate();
                         yybegin(EOL_POP);
                         return AsciiDocTokenTypes.CONTINUATION;
                       }
}

// everything that will not render after a [source] as literal text
// especially: titles, block attributes, block ID, etc.
<PREBLOCK, DELIMITER, LIST> {
  ^ [ \t]+ / ({BULLET} {SPACE}+ {STRING} | {ENUMERATION} {SPACE}+ {STRING} | {DESCRIPTION} {SPACE}+ {STRING} | {DESCRIPTION} {SPACE}* "\n" )  {
        yybegin(LIST); return AsciiDocTokenTypes.WHITE_SPACE;
      }
}
<PREBLOCK> {
  ^ [ \t]+ [^ \t\n] {
        if (style == null && !isPrefixedBy(new char[] {tableChar})) { // when running incremental lexing, this will be a false-positive for a beginning of the line for a cell
          yybegin(LISTING_NO_DELIMITER); return AsciiDocTokenTypes.LISTING_TEXT;
        } else {
          yypushback(yylength()); yybegin(STARTBLOCK);
        }
      }
  ^ {SPACE}* "\n"           {
        while (isNoDelList()) { blockStack.pop(); }
        if (isNoDel()) { blockStack.pop(); }
        resetFormatting();
        if (style == null && blockStack.size() == 0 && stateStack.size() == 0 && tableChar == 0) {
          yybegin(YYINITIAL);
        } else {
          yybegin(MULTILINE);
        }
        return AsciiDocTokenTypes.EMPTY_LINE;
      }
  {SPACE}* "\n"           { if (isNoDel()) { blockStack.pop(); } resetFormatting(); yybegin(MULTILINE); return AsciiDocTokenTypes.LINE_BREAK; } // blank lines within pre block don't have an effect
  ^ {TITLE_START} {CELLPREFIX} "|"  {
                         if (yycharat(yylength() - 1) == tableChar && !isEscaped()) {
                           // we're inside a table, stop here and continue in SINGLELINE to do cell logic
                           yypushback(yylength());
                           yybegin(SINGLELINE);
                         } else {
                           // we're not inside a table. As it starts with a dot, it's a title
                           yypushback(yylength() - 1);
                           resetFormatting(); yybegin(TITLE); return AsciiDocTokenTypes.TITLE_TOKEN;
                         }
                       }
  ^ {TITLE_START} / [^ \t] { resetFormatting(); yybegin(TITLE); return AsciiDocTokenTypes.TITLE_TOKEN; }
}

<HEADER, PREBLOCK> {
  {ATTRIBUTE_NAME_START} / "!"? {AUTOCOMPLETE}? {ATTRIBUTE_NAME_DECL} "!"? {ATTRIBUTE_NAME_END} {
        if (!isEscaped()) {
          yypushstate();
          yybegin(ATTRIBUTE_NAME);
          return AsciiDocTokenTypes.ATTRIBUTE_NAME_START;
        } else {
          yypushback(yylength()); yybegin(STARTBLOCK);
        }
      }
  {ATTRIBUTE_NAME_START} / [^:\n \t]* {AUTOCOMPLETE} {
    if (!isEscaped()) {
      yypushstate();
      yybegin(ATTRIBUTE_NAME); return AsciiDocTokenTypes.ATTRIBUTE_NAME_START;
    } else {
      return textFormat();
    }
  }
}

<EOL_POP> {
  "\n" {
    yypopstate();
    return AsciiDocTokenTypes.LINE_BREAK;
  }
  {SPACE} {
    return AsciiDocTokenTypes.WHITE_SPACE;
  }
  [^] {
    yypopstate();
    yypushback(yylength());
  }
}

<DELIMITER, LIST, PREBLOCK, HEADER> {
  {IFDEF_IFNDEF} / [^ \[\n] { yypushstate(); yybegin(IFDEF_IFNDEF_ENDIF); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  // endif/ifeval allows the body to be empty, special case...
  ^ ("endif"|"ifeval") "::" / [^ \n] { yypushstate(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
}

<HEADING, DOCTITLE> {
    {INLINEIDSTART} / [^\]\n]+ {INLINEIDEND} {
                           if (!isEscaped()) {
                             yypushstate(); yybegin(INLINEID); return AsciiDocTokenTypes.INLINEIDSTART;
                           } else {
                             yypushback(1);
                             return AsciiDocTokenTypes.HEADER;
                           }
                         }
}

<HEADER, INSIDE_HEADER_LINE> {
  ^{SPACE}* "\n" {
        yypushback(yylength());
        yybegin(PREBLOCK);
  }
  "\n" {
        ++ headerLines;
        if (headerLines >= 3) {
          yybegin(PREBLOCK);
        } else {
          yybegin(HEADER);
        }
        return AsciiDocTokenTypes.LINE_BREAK;
  }
}

<HEADER> {
  [ \t] {
        return AsciiDocTokenTypes.HEADER;
      }
  [^] {
        yybegin(INSIDE_HEADER_LINE);
        return AsciiDocTokenTypes.HEADER;
  }
}

<INSIDE_HEADER_LINE> {
  [^] {
        return AsciiDocTokenTypes.HEADER;
  }
}

<PREBLOCK> {
  ^ {PAGEBREAK} $ { resetFormatting(); yybegin(PREBLOCK); return AsciiDocTokenTypes.PAGEBREAK; }
  ^ {HORIZONTALRULE} $ { resetFormatting(); yybegin(PREBLOCK); return AsciiDocTokenTypes.HORIZONTALRULE; }
  {BLOCK_MACRO_START} / [^ \[\n] { yypushstate(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  // toc allows the body to be empty, special case...
  ^ {HEADING_START} | {HEADING_START_MARKDOWN} / {NON_SPACE} { if (blockStack.size() == 0) {
                              int level = 0;
                              CharSequence prefix = yytext();
                              while (prefix.length() > level && (prefix.charAt(level) == '#' || prefix.charAt(level) == '=')) {
                                ++ level;
                              }
                              if (level == 1 && style == null) {
                                yybegin(DOCTITLE);
                              } else {
                                yybegin(HEADING);
                              }
                              clearStyle(); resetFormatting(); return AsciiDocTokenTypes.HEADING_TOKEN;
                            }
                            yypushback(yylength()); yybegin(STARTBLOCK);
                          }
}

<DELIMITER, PREBLOCK, LIST> {
  ^ ("toc") "::" / [^ \n] { yypushstate(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  // if it starts like a block attribute, but has characters after the closing bracket, it's not
  {BLOCK_ATTRS_START} / [^\[\#] ([^\]\"\']* | (\" [^\"\n]* \") | (\' [^\"\n]* \') )* "]" [ \t]* [^\n] { yypushback(yylength()); yybegin(STARTBLOCK); }
  {BLOCK_ATTRS_START} / [^\[] { yybegin(MULTILINE); yypushstate(); clearStyle(); yybegin(BLOCK_ATTRS); return AsciiDocTokenTypes.ATTRS_START; }
  {ANCHORSTART} / [^\]\n]+ {ANCHOREND} { resetFormatting(); yybegin(ANCHORID); return AsciiDocTokenTypes.BLOCKIDSTART; }
  {BLOCKIDSTART} / [^\[\]\n]* {BLOCKIDEND} {
                         if (!isEscaped()) {
                           yybegin(BLOCKID); return AsciiDocTokenTypes.BLOCKIDSTART;
                         } else {
                           yypushback(1);
                           return AsciiDocTokenTypes.LBRACKET;
                         }
                       }
  // triple rules to handle EOF
  ^ {LISTING_BLOCK_DELIMITER} $ { clearStyle(); resetFormatting(); yybegin(LISTING_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER; }
  ^ {LISTING_BLOCK_DELIMITER} {SPACE}+ "-" { yypushback(yylength()); yybegin(STARTBLOCK);  }
  ^ {LISTING_BLOCK_DELIMITER} / [^\-\n \t] { yypushback(yylength()); yybegin(STARTBLOCK);  }
  ^ {LISTING_BLOCK_DELIMITER} | {MARKDOWN_LISTING_BLOCK_DELIMITER} { clearStyle(); resetFormatting(); yybegin(LISTING_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER; }

  ^ {PASSTRHOUGH_BLOCK_DELIMITER} $ { clearStyle(); resetFormatting(); yybegin(PASSTRHOUGH_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER; }
  ^ {PASSTRHOUGH_BLOCK_DELIMITER} / [^\+\n \t] { yypushback(yylength()); yybegin(STARTBLOCK);  }
  ^ {PASSTRHOUGH_BLOCK_DELIMITER} { clearStyle(); resetFormatting(); yybegin(PASSTRHOUGH_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER; }

  ^ {TABLE_BLOCK_DELIMITER} $ {
            resetFormatting();
            style = null;
            if (tableChar == 0) {
              tableChar = yycharat(0);
              zzEndReadL = limitLookahead(zzCurrentPosL);
            } else {
              tableChar = 0;
              zzEndReadL = limitLookahead(zzCurrentPosL);
            }
            yybegin(PREBLOCK);
            yypushstate();
            yybegin(EOL_POP);
            return AsciiDocTokenTypes.BLOCK_DELIMITER;
          }

  ^ ({EXAMPLE_BLOCK_DELIMITER} | {QUOTE_BLOCK_DELIMITER} | {SIDEBAR_BLOCK_DELIMITER} | {OPEN_BLOCK_DELIMITER}) $ {
                            resetFormatting();
                            String delimiter = yytext().toString().trim();
                            if(blockStack.contains(delimiter)) {
                              while (blockStack.contains(delimiter)) {
                                blockStack.pop();
                              }
                            } else {
                              if (delimiter.equals("--") && ("source".equals(style) || "plantuml".equals(style))) {
                                 clearStyle(); yybegin(LISTING_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER;
                              }
                              blockStack.push(delimiter);
                            }
                            yybegin(PREBLOCK);
                            yypushstate();
                            yybegin(EOL_POP);
                            return AsciiDocTokenTypes.BLOCK_DELIMITER;
                          }
  ^ {QUOTE_BLOCK_DELIMITER} / [^\_\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); /* QUOTE_BLOCK_DELIMITER */ }
  ^ {SIDEBAR_BLOCK_DELIMITER} / [^\*\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); /* SIDEBAR_BLOCK_DELIMITER */ }
  ^ {TABLE_BLOCK_DELIMITER} / [^\=\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); /* TABLE_BLOCK_DELIMITER */ }
  ^ {OPEN_BLOCK_DELIMITER} / [^\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); /* OPEN_BLOCK_DELIMITER */ }
  ^ {EXAMPLE_BLOCK_DELIMITER} / [^\=\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); /* EXAMPLE_BLOCK_DELIMITER */ }

  ^ {TABLE_BLOCK_DELIMITER} {
            resetFormatting();
            style = null;
            if (tableChar == 0) {
              tableChar = yycharat(0);
              zzEndReadL = limitLookahead(zzCurrentPosL);
            } else {
              tableChar = 0;
              zzEndReadL = limitLookahead(zzCurrentPosL);
            }
            yybegin(PREBLOCK);
            yypushstate();
            yybegin(EOL_POP);
            return AsciiDocTokenTypes.BLOCK_DELIMITER;
          }

  ^ ({EXAMPLE_BLOCK_DELIMITER} | {QUOTE_BLOCK_DELIMITER} | {SIDEBAR_BLOCK_DELIMITER} | {OPEN_BLOCK_DELIMITER})  {
                            resetFormatting();
                            String delimiter = yytext().toString().trim();
                            if(blockStack.contains(delimiter)) {
                              while (blockStack.contains(delimiter)) {
                                blockStack.pop();
                              }
                            } else {
                              if (delimiter.equals("--") && "source".equals(style)) {
                                 clearStyle(); yybegin(LISTING_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER;
                              }
                              blockStack.push(delimiter);
                            }
                            yybegin(PREBLOCK);
                            yypushstate();
                            yybegin(EOL_POP);
                            return AsciiDocTokenTypes.BLOCK_DELIMITER;
                          }

  ^ {LITERAL_BLOCK_DELIMITER} $ { clearStyle(); resetFormatting(); yybegin(LITERAL_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER; }
  ^ {LITERAL_BLOCK_DELIMITER} / [^\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); }
  ^ {LITERAL_BLOCK_DELIMITER} { clearStyle(); resetFormatting(); yybegin(LITERAL_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER; }
}

<DELIMITER, LIST> {
  {SPACE}* "\n"           { yypushback(yylength()); yybegin(PREBLOCK); } // blank lines don't have an effect
}

<LIST> {
  [^]                  { yypushback(yylength()); yybegin(INSIDE_LINE); }
}

<DELIMITER> {
  [^] {
    yypushback(yylength()); yybegin(SINGLELINE);
  }
}

// no pre-block or boundary elements matched, now start block with the style that has been defined
<PREBLOCK, STARTBLOCK> {
  [^] {
        resetFormatting();
        if (style == null) {
          yypushback(yylength()); yybegin(SINGLELINE);
        } else if ("source".equals(style)) {
          yypushback(yylength()); yybegin(LISTING_NO_DELIMITER);
        } else if ("pass".equals(style)) {
          yypushback(yylength()); yybegin(PASSTHROUGH_NO_DELIMITER);
        } else {
          blockStack.push("nodel-" + style);
          yypushback(yylength()); yybegin(SINGLELINE);
        }
        clearStyle();
      }
}

<SINGLELINE, LISTING_NO_DELIMITER> {
  // this will only terminate any open blocks when they appear even in no-delimiter blocks
  ^ ({EXAMPLE_BLOCK_DELIMITER} | {QUOTE_BLOCK_DELIMITER} | {SIDEBAR_BLOCK_DELIMITER} | {TABLE_BLOCK_DELIMITER} | {OPEN_BLOCK_DELIMITER}) $ {
                            String delimiter = yytext().toString().trim();
                            if(blockStack.contains(delimiter)) {
                              while (blockStack.contains(delimiter)) {
                                blockStack.pop();
                              }
                              yybegin(PREBLOCK);
                              yypushstate();
                              yybegin(EOL_POP);
                              return AsciiDocTokenTypes.BLOCK_DELIMITER;
                            } else {
                              yybegin(INSIDE_LINE);
                              return textFormat();
                            }
                          }
}

<SINGLELINE> {
  "[" [^\]\n]+ "]" / "#" { return textFormat(); } // attribute, not handled yet
  ^ {ADMONITION} / {SPACE}+ {STRING} { resetFormatting(); yybegin(INSIDE_LINE); return AsciiDocTokenTypes.ADMONITION; }
  /* a blank line, it separates blocks. Don't return YYINITIAL here, as writing on a blank line might change the meaning
  of the previous blocks combined (for example there is now an italic formatting spanning the two combined blocks) */
  ^ {SPACE}* "\n"           { clearStyle();
                         resetFormatting();
                         if (blockStack.size() == 0) {
                           if (stateStack.size() == 0 && tableChar == 0) {
                             yybegin(YYINITIAL);
                           } else {
                             yybegin(MULTILINE);
                           }
                         } else if (isNoDel()) {
                           blockStack.pop();
                           if (blockStack.size() == 0) {
                             if (stateStack.size() == 0 && tableChar == 0) {
                               yybegin(YYINITIAL);
                             } else {
                               yybegin(MULTILINE);
                             }
                           } else {
                             yybegin(MULTILINE);
                           }
                         } else {
                           yybegin(DELIMITER);
                         }
                         return AsciiDocTokenTypes.EMPTY_LINE;
                       }
  {SPACE}* "\n"           { clearStyle();
                         resetFormatting();
                         if (blockStack.size() == 0) {
                           yybegin(MULTILINE);
                         } else if (isNoDel()) {
                           blockStack.pop();
                           yybegin(MULTILINE);
                         } else {
                           yybegin(DELIMITER);
                         }
                         return AsciiDocTokenTypes.LINE_BREAK;
                       }
  ^ "::"                 { yybegin(INSIDE_LINE); return textFormat(); } // avoid end-of-sentence
  [ \t]+               { yybegin(INSIDE_LINE);
                         if (singlemono || doublemono) {
                           return AsciiDocTokenTypes.WHITE_SPACE_MONO;
                         } else {
                           return AsciiDocTokenTypes.WHITE_SPACE;
                         }
                       }
  {CONTINUATION} {SPACE}* "\n" {
                         yypushback(yylength() - 1);
                         yybegin(PREBLOCK);
                         yypushstate();
                         yybegin(EOL_POP);
                         return AsciiDocTokenTypes.CONTINUATION;
                       }
  {CELLPREFIX} "|"    {  zzEndReadL = limitLookahead(zzCurrentPosL);
                         if (yycharat(yylength() - 1) == tableChar && !isEscaped()) {
                           if (isNoDelVerse()) {
                             yybegin(SINGLELINE);
                           } else {
                             if (blockStack.size() == 0 && stateStack.size() == 0 && style == null) {
                               yybegin(YYINITIAL);
                             } else {
                               yybegin(SINGLELINE);
                             }
                           }
                           resetFormatting();
                           return AsciiDocTokenTypes.CELLSEPARATOR;
                         } else {
                           yypushback(yylength()); yybegin(INSIDE_LINE);
                         }
                       }
  [^]                  { yypushback(yylength()); yybegin(INSIDE_LINE); }
}

<LINECOMMENT> {
  "\n" {
    yypopstate();
    return AsciiDocTokenTypes.LINE_BREAK;
  }
  [^] {
    return AsciiDocTokenTypes.LINE_COMMENT;
  }
}

<PREBLOCK, SINGLELINE, PASSTHROUGH_NO_DELIMITER, HEADER, LIST> {
  {LINE_COMMENT} {
    // the line comment might be stopped while reading a comment within a table up until the cell separator
    if (tableChar != 0) {
      zzEndReadL = limitLookahead(zzCurrentPosL);
    }
    yypushstate();
    yybegin(LINECOMMENT);
    return AsciiDocTokenTypes.LINE_COMMENT;
  }
}

<PREBLOCK, SINGLELINE, DELIMITER, HEADER> {
  {COMMENT_BLOCK_DELIMITER} $ { clearStyle(); resetFormatting(); yypushstate(); yybegin(COMMENT_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.BLOCK_COMMENT; }
  {COMMENT_BLOCK_DELIMITER} / [^\/\n \t] { yypushback(yylength()); yybegin(STARTBLOCK);  }
  {COMMENT_BLOCK_DELIMITER} { clearStyle(); resetFormatting(); yypushstate(); yybegin(COMMENT_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.BLOCK_COMMENT; }
}

<BIBSTART> {
  {SPACE} { return AsciiDocTokenTypes.WHITE_SPACE; }
  {BIBSTART} { yybegin(BIBID); return AsciiDocTokenTypes.BIBSTART; }
}

<BIBID> {
  "," { yybegin(BIBNAME); return AsciiDocTokenTypes.SEPARATOR; }
}

<BIBNAME> {
  "]" {BIBEND} { return AsciiDocTokenTypes.BLOCKREFTEXT; }
}

<BIBNAME, BIBID> {
  {BIBEND} { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.BIBEND; }
}

<BIBNAME> {
  [^] { return AsciiDocTokenTypes.BLOCKREFTEXT; }
}

<BIBID> {
  [^] { return AsciiDocTokenTypes.BLOCKID; }
}

<DESCRIPTION> {
  {DESCRIPTION_END} / [ \t\n] { yypopstate(); return AsciiDocTokenTypes.DESCRIPTION; }
}

<TITLE> {
  // needs to be before the newline defined for INSIDE_LINE, DESCRIPTION, TITLE
  "\n"                 { yyinitialIfNotInBlock(); return AsciiDocTokenTypes.LINE_BREAK; }
}

<KBD_MACRO_ATTRS> {
  "++" + "++" {
        return AsciiDocTokenTypes.INLINE_MACRO_BODY;
      }
  "++" [^+] ({STRINGNOPLUS} | [\+][^\+])* "++" {
                         if (isEscaped()) {
                           yypushback(yylength() - 1);
                           return textFormat();
                         } else {
                           yypushback(yylength() - 2);
                           yypushstate(); yybegin(PASSTRHOUGH_INLINE_CONSTRAINED); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START;
                         }
                       }
  "+" [^+\n \t] ({WORDNOPLUS} | [ \t][\+] | [\+][\p{Letter}\p{Digit}_])* "+" {
              yypushback(yylength() - 1);
              yypopstate();
              if(isUnconstrainedStart()) {
                yypushstate(); yybegin(PASSTRHOUGH_INLINE_UNCONSTRAINED); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START;
              } else {
                yypushback(yylength() - 1);
                return textFormat();
         }
       }
  {PASSTRHOUGH_INLINE} ({STRINGNOPLUS} | [^\+][^\+]{1,2}[^+] )* {PASSTRHOUGH_INLINE} {
                           if (isEscaped()) {
                             yypushback(yylength() - 2);
                             return textFormat();
                           } else {
                             yypushback(yylength() - 3);
                             yypushstate(); yybegin(PASSTRHOUGH_INLINE); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START;
                           }
                         }
  "]"                  { yypushback(yylength()); yypopstate(); }
  {SPACE} { return AsciiDocTokenTypes.WHITE_SPACE; }
  [^] {
          yypushstate();
          yybegin(KBD_MACRO_ATTRS_KEY);
          return AsciiDocTokenTypes.INLINE_MACRO_BODY;
      }
}

<KBD_MACRO_ATTRS_KEY> {
  "++" [^+] ({STRINGNOPLUS} | [\+][^\+])* "++" {
                         if (isEscaped()) {
                           yypushback(yylength() - 1);
                           return textFormat();
                         } else {
                           yypushback(yylength() - 2);
                           yypushstate(); yybegin(PASSTRHOUGH_INLINE_CONSTRAINED); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START;
                         }
                       }
  "]" {
      yypushback(yylength());
      yypopstate();
  }
  {SPACE}* [+,] {SPACE}* {
      yypopstate();
      return AsciiDocTokenTypes.SEPARATOR;
  }
  [^] {
      return AsciiDocTokenTypes.INLINE_MACRO_BODY;
  }
}

<INSIDE_LINE, DESCRIPTION, TITLE> {
  // PASSTHROUGH START
  "++" [^+] ({STRINGNOPLUS} | [^\+][\+][^\+])* "++" {
                         if (isEscaped()) {
                           yypushback(yylength() - 1);
                           return textFormat();
                         } else {
                           yypushback(yylength() - 2);
                           yypushstate(); yybegin(PASSTRHOUGH_INLINE_CONSTRAINED); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START;
                         }
                       }
  "+" {
                       if(isUnconstrainedStart() && !isEscaped()) {
                         yypushback(yylength());
                         yypushstate();
                         yybegin(PASSTHROUGH_SECOND_TRY);
                       } else {
                         return textFormat();
                       }
  }
  {PASSTRHOUGH_INLINE} ({STRINGNOPLUS} | [^\+][^\+]{1,2}[^+] )* {PASSTRHOUGH_INLINE} {
                           if (isEscaped()) {
                             yypushback(yylength() - 2);
                             return textFormat();
                           } else {
                             yypushback(yylength() - 3);
                             yypushstate(); yybegin(PASSTRHOUGH_INLINE); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START;
                           }
                         }
  // PASSTHROUGH END
}

<INSIDE_LINE, DESCRIPTION, TITLE> {
  "\n"                 { if (isNoDelList()) {
                           yybegin(LIST);
                         } else if (isNoDel()) {
                           yybegin(SINGLELINE);
                         } else {
                           yybegin(DELIMITER);
                         }
                         return AsciiDocTokenTypes.LINE_BREAK; }
  {CELLPREFIX} "|"     { zzEndReadL = limitLookahead(zzCurrentPosL);
                         if (tableChar == yycharat(yylength()-1) && !isEscaped()) {
                           if (yylength() > 1 && !isPrefixedBy(SPACES)) {
                             yypushback(1);
                             return textFormat();
                           } else if (isNoDelVerse()) {
                             yybegin(SINGLELINE);
                           } else {
                             if (blockStack.size() == 0 && stateStack.size() == 0 && style == null) {
                               yybegin(YYINITIAL);
                             } else {
                               yybegin(SINGLELINE);
                             }
                           }
                           resetFormatting();
                           return AsciiDocTokenTypes.CELLSEPARATOR;
                         } else {
                           return textFormat();
                         }
                       }
  {HARD_BREAK}
                       { yypushback(1); return AsciiDocTokenTypes.HARD_BREAK; }
  // exceptions to END_OF_SENTENCE
  [:letter:] "." " "? [:letter:] "." { return textFormat(); } // i.e., e.g., ...
  "Dr." | "Prof." | "Ing." / {SPACE}* [^ \t\n] { return textFormat(); } // title inside a line as text if inside of a line
  \p{Uppercase} "." / {SPACE}* [^ \t\n] { return textFormat(); } // initials inside a line as text if inside of a line
  ".." "."* / {SPACE}* [^ \t\n] { return textFormat(); } // avoid end of sentence for "..." if inside of a line
  {END_OF_SENTENCE} / {SPACE}+ [^\p{Uppercase}\n]* [\p{Lowercase}\p{Digit}] // standard text if followed by lower case character or digit
                       { return textFormat(); }
  ({END_OF_SENTENCE} | (" "? ":")) / {SPACE}* \n // end of sentence at end of line
                       { if (!doublemono && !singlemono) {
                           return AsciiDocTokenTypes.END_OF_SENTENCE;
                         } else {
                           return textFormat();
                         }
                       }
  {END_OF_SENTENCE} / {SPACE} // end of sentence within a line, needs to be unconstrained
                       { if (!doublemono && !singlemono && isUnconstrainedEnd() && !isPrefixedBy(NUMBERS)) {
                           return AsciiDocTokenTypes.END_OF_SENTENCE;
                         } else {
                           return textFormat();
                         }
                       }
  [ \t]                { if (singlemono || doublemono) {
                           return AsciiDocTokenTypes.WHITE_SPACE_MONO;
                         } else {
                           return AsciiDocTokenTypes.WHITE_SPACE;
                         }
                       }
  // BOLD START
  // start something with ** only if it closes within the same block
  {DOUBLEBOLD} [^\*] ({STRINGNOASTERISK} | [^\*][\*][^\*])* {DOUBLEBOLD} {
                         yypushback(yylength() -2 );
                         if(!singlebold) {
                            doublebold = !doublebold; return doublebold ? AsciiDocTokenTypes.BOLD_START : AsciiDocTokenTypes.BOLD_END;
                         } else {
                            return textFormat();
                         }
                       }
  {DOUBLEBOLD}         { if(doublebold && !singlebold) {
                           doublebold = false; return AsciiDocTokenTypes.BOLD_END;
                         } else {
                           yypushback(1);
                           return textFormat();
                         }
                       }
  {BOLD} {BOLD}? [^\*\n \t] ({WORDNOASTERISK} | [ \t][\*][^\*] | [\*][\p{Letter}\p{Digit}_])* {BOLD} {
                         if (doublebold && yytext().toString().startsWith("**")) {
                           yypushback(yylength() - 2);
                           doublebold = false; return AsciiDocTokenTypes.BOLD_END;
                         } else {
                           yypushback(yylength() - 1);
                           if(isUnconstrainedStart() && !singlebold && !doublebold) {
                             singlebold = true; return AsciiDocTokenTypes.BOLD_START;
                           } else if (singlebold && isUnconstrainedEnd()) {
                             singlebold = false; return AsciiDocTokenTypes.BOLD_END;
                           } else {
                             return textFormat();
                           }
                         }
                       }
  {BOLD}               { if(singlebold && !doublebold && isUnconstrainedEnd()) {
                           singlebold = false; return AsciiDocTokenTypes.BOLD_END;
                         } else {
                           return textFormat();
                         }
                       }
  // BOLD END

  // ITALIC START
  // start something with __ only if it closes within the same block
  {DOUBLEITALIC} [^\_] ({STRINGNOUNDERSCORE} | [^\_][\_][^\_])* {DOUBLEITALIC} {
                         yypushback(yylength() - 2);
                         if(!singleitalic) {
                            doubleitalic = !doubleitalic; return doubleitalic ? AsciiDocTokenTypes.ITALIC_START : AsciiDocTokenTypes.ITALIC_END;
                         } else {
                            return textFormat();
                         }
                       }
  {DOUBLEITALIC}         { if(doubleitalic && !singleitalic) {
                           doubleitalic = false; return AsciiDocTokenTypes.ITALIC_END;
                         } else {
                           yypushback(1);
                           return textFormat();
                         }
                       }
  {ITALIC} {ITALIC}? [^\_\n \t] ({WORDNOUNDERSCORE} | [ \t][\_][^\_] | [\_][\p{Letter}\p{Digit}_])* {ITALIC} {
                         if (doubleitalic && yytext().toString().startsWith("__")) {
                           yypushback(yylength() - 2);
                           doubleitalic = false; return AsciiDocTokenTypes.ITALIC_END;
                         } else {
                           yypushback(yylength() - 1);
                           if(isUnconstrainedStart() && !singleitalic && !doubleitalic) {
                              singleitalic = true; return AsciiDocTokenTypes.ITALIC_START;
                           } else if (singleitalic && isUnconstrainedEnd()) {
                              singleitalic = false; return AsciiDocTokenTypes.ITALIC_END;
                           } else {
                              return textFormat();
                           }
                         }
                       }
  {ITALIC}               { if(singleitalic && !doubleitalic && isUnconstrainedEnd()) {
                           singleitalic = false; return AsciiDocTokenTypes.ITALIC_END;
                         } else {
                           return textFormat();
                         }
                       }
  // ITALIC END

  // MONO START
  // start something with ** only if it closes within the same block
  {DOUBLEMONO} [^\`] ({STRINGNOBACKTICK} | [^\`][\`][^\`])* {DOUBLEMONO} {
                         yypushback(yylength() - 2);
                         if(!singlemono) {
                            doublemono = !doublemono; return doublemono ? AsciiDocTokenTypes.MONO_START : AsciiDocTokenTypes.MONO_END;
                         } else {
                            return textFormat();
                         }
                       }
  {DOUBLEMONO}         { if(doublemono && !singlemono) {
                           doublemono = false; return AsciiDocTokenTypes.MONO_END;
                         } else {
                           yypushback(1);
                           return textFormat();
                         }
                       }
  {MONO}               { if(singlemono && !doublemono && isUnconstrainedEnd()) {
                           singlemono = false; return AsciiDocTokenTypes.MONO_END;
                         } else {
                           // might be a starting MONO, give it a second try
                           // didn't use look-ahead here to avoid problems with {TYPOGRAPHIC_DOUBLE_QUOTE_END}
                           yypushback(yylength());
                           yypushstate();
                           yybegin(MONO_SECOND_TRY);
                         }
                       }
  // MONO END

  {LPAREN}             { return AsciiDocTokenTypes.LPAREN; }
  {RPAREN}             { return AsciiDocTokenTypes.RPAREN; }
  {LBRACKET}           { return AsciiDocTokenTypes.LBRACKET; }
  {RBRACKET}           { if (isInAttribute()) { yypushback(1); yypopstate(); } else { return AsciiDocTokenTypes.RBRACKET; } }
  // see: InlineXrefMacroRx
  {REFSTART} / [\w/.:{#] [^>\n]* {REFEND} {
                         if (!isEscaped()) {
                           yybegin(REF); return AsciiDocTokenTypes.REFSTART;
                         } else {
                           yypushback(1);
                           return AsciiDocTokenTypes.LT;
                         }
                       }
  // when typing a reference, it will not be complete due to the missing matching closing ref
  // therefore second variant for incomplete REF that will only be active during autocomplete
  {REFSTART} / ([\w/.:{#] [^>\n]* | "") {AUTOCOMPLETE} {
                         if (!isEscaped()) {
                           yybegin(REFAUTO); return AsciiDocTokenTypes.REFSTART;
                         } else {
                           yypushback(1);
                           return AsciiDocTokenTypes.LT;
                         }
                        }
  (->|=>|<-|<=)        { return AsciiDocTokenTypes.ARROW; } // avoid errors to be recognized as LT/GT
  {LT}                 { return AsciiDocTokenTypes.LT; }
  {GT}                 { return AsciiDocTokenTypes.GT; }
  {SINGLE_QUOTE}       { if (isUnconstrainedStart() || isUnconstrainedEnd()) {
                           return AsciiDocTokenTypes.SINGLE_QUOTE;
                         } else {
                           return textFormat();
                         }
                       }
  {DOUBLE_QUOTE}       { return AsciiDocTokenTypes.DOUBLE_QUOTE; }
  {TYPOGRAPHIC_DOUBLE_QUOTE_START} [^\*\n \t] {WORD}* {TYPOGRAPHIC_DOUBLE_QUOTE_END} {
                           yypushback(yylength() - 2);
                           if (isUnconstrainedStart()) {
                             typographicquote = true;
                             return AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_START;
                           } else {
                             yypushback(1);
                             return AsciiDocTokenTypes.DOUBLE_QUOTE;
                           }
                         }
  {TYPOGRAPHIC_DOUBLE_QUOTE_END} {
                           // `" might be a typographic quote end of the start of a monospaced quoted part
                           // if it doesn't match, give MONO start a second try.
                           if (typographicquote && isUnconstrainedEnd()) {
                             typographicquote = false;
                             return AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_END;
                           } else {
                             yypushback(yylength());
                             yypushstate();
                             yybegin(MONO_SECOND_TRY);
                           }
                         }
  {TYPOGRAPHIC_SINGLE_QUOTE_START} [^\*\n \t] {WORD}* {TYPOGRAPHIC_SINGLE_QUOTE_END} {
                           yypushback(yylength() - 2);
                           if (isUnconstrainedStart()) {
                             typographicquote = true;
                             return AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START;
                           } else {
                             yypushback(1);
                             return AsciiDocTokenTypes.SINGLE_QUOTE;
                           }
                         }
  {TYPOGRAPHIC_SINGLE_QUOTE_END} {
                           if (typographicquote && isUnconstrainedEnd()) {
                             typographicquote = false;
                             return AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_END;
                           } else {
                             yypushback(yylength());
                             yypushstate();
                             yybegin(MONO_SECOND_TRY);
                           }
                         }
  {INLINE_URL_NO_DELIMITER} {
        if (isEscaped() || isPrefixedBy(COLONSLASH)) {
          yypushback(yylength() - 1);
          return textFormat();
        } else {
          yypushstate();
          yypushback(yylength());
          yybegin(INLINE_URL_NO_DELIMITER);
        }
  }
  {INLINE_EMAIL_NO_DELIMITER} {
        if (isEscaped() || isPrefixedBy(COLONSLASH)) {
          return textFormat();
        } else {
          return AsciiDocTokenTypes.URL_EMAIL;
        }
  }
  "<" / {INLINE_URL_WITH_DELIMITER} ">" [^\[] {
        yypushstate();
        yybegin(INLINE_URL_IN_BRACKETS);
        return AsciiDocTokenTypes.URL_START;
  }
  "<" / {INLINE_URL_WITH_DELIMITER} ">" "[" {
        return AsciiDocTokenTypes.LT;
  }
  "<" / {INLINE_URL_WITH_DELIMITER} ">" {
        yypushstate();
        yybegin(INLINE_URL_IN_BRACKETS);
        return AsciiDocTokenTypes.URL_START;
  }
  mailto: / [^\[\n \t]* "[" [^\n]* "]"  {
        yypushstate();
        yybegin(INLINE_EMAIL_WITH_PREFIX);
        return AsciiDocTokenTypes.URL_PREFIX;
  }
  // allow autocomplete even if brackets have not been entered yet
  ({LINKSTART} | {XREFSTART}) / ([^\[\n \t] | {AUTOCOMPLETE})* ("+++" [^+] {WORD}* "+++")* ("++" {WORD}* "++")* ( {AUTOCOMPLETE} | {AUTOCOMPLETE}? {LINKTEXT_START} {WORDNOBRACKET}* {LINKEND}) {
                         if (!isEscaped()) {
                           yypushstate(); yybegin(LINKFILE); return AsciiDocTokenTypes.LINKSTART;
                         } else {
                           return textFormat();
                         }
                       }
  {INLINE_MACRO_START} / ({INLINE_URL_NO_DELIMITER} | {INLINE_URL_WITH_DELIMITER}) ({AUTOCOMPLETE} | {AUTOCOMPLETE}? "[" {WORDNOBRACKET}* "]") {
        if (!isEscaped()) {
          yypushstate();
          yybegin(INLINE_MACRO_URL);
          return AsciiDocTokenTypes.INLINE_MACRO_ID;
        } else {
          return textFormat();
        }
      }
  {INLINE_MACRO_START} / ([^ \[\n\"`:/] [^\s\[\n\"`]* | "") ({AUTOCOMPLETE} | {AUTOCOMPLETE}? "[" ({WORDNOBRACKET}*|"[" [^\]\n]* "]")*  "]") {
        if (!isEscaped()) {
          yypushstate();
          if (yytext().toString().equals("xref:")) {
            // this might be an incomplete xref with autocomplete as this pattern is less strict than the xref pattern
            yybegin(LINKFILE);
            return AsciiDocTokenTypes.LINKSTART;
          } else if (yytext().toString().equals("kbd:")) {
            // kbd may have passthrough sequences
            yybegin(KBD_MACRO);
            return AsciiDocTokenTypes.INLINE_MACRO_ID;
          } else {
            yybegin(INLINE_MACRO);
            return AsciiDocTokenTypes.INLINE_MACRO_ID;
          }
        } else {
          return textFormat();
        }
      }
  // support for blanks in well-known macros as long as the target doesn't contain a colon (that could indicate a the next real macro)
  {INLINE_MACRO_START} / ([^ \[\n\"`:/] [^\[\n\"`:]* | "") "[" ({WORDNOBRACKET}*|"[" [^\]\n]* "]")*  "]" {
          if (!isEscaped()) {
            yypushstate();
            if (yytext().toString().equals("xref:") || yytext().toString().equals("menu:")) {
              // this might be an xref with a blank as this pattern is less strict than the xref pattern
              yybegin(LINKFILEWITHBLANK);
              return AsciiDocTokenTypes.LINKSTART;
            } else if (AsciiDocInlineMacro.HAS_FILE_AS_BODY.contains(zzBuffer.subSequence(zzStartRead, zzMarkedPos - 1).toString())) {
              yybegin(INLINE_MACRO);
              return AsciiDocTokenTypes.INLINE_MACRO_ID;
            } else {
              yypushback(1);
              return textFormat();
            }
          } else {
            return textFormat();
          }
        }
  "&#" [0-9]+ ";" {
          if (!isEscaped()) {
            return AsciiDocTokenTypes.HTML_ENTITY_OR_UNICODE;
          } else {
            return textFormat();
          }
        }
  "&#x" [0-9A-Fa-f]+ ";" {
          if (!isEscaped()) {
            return AsciiDocTokenTypes.HTML_ENTITY_OR_UNICODE;
          } else {
            return textFormat();
          }
        }
  "&" [A-Za-z]+ ";" {
          if (!isEscaped()) {
            return AsciiDocTokenTypes.HTML_ENTITY_OR_UNICODE;
          } else {
            return textFormat();
          }
        }
  {INLINEIDSTART} / [^\]\n]+ {INLINEIDEND} {
                         if (!isEscaped()) {
                           yypushstate(); yybegin(INLINEID); return AsciiDocTokenTypes.INLINEIDSTART;
                         } else {
                           yypushback(1);
                           return AsciiDocTokenTypes.LBRACKET;
                         }
                       }

  [^]                  { return textFormat(); }
}

<PASSTHROUGH_SECOND_TRY> {
  "+" [^+\n \t] ({WORDNOPLUS} | [ \t][\+] | [\+][\p{Letter}\p{Digit}_])* "+" {
              yypushback(yylength() - 1);
              yypopstate();
              if(isUnconstrainedStart()) {
                yypushstate(); yybegin(PASSTRHOUGH_INLINE_UNCONSTRAINED); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START;
              } else {
                yypushback(yylength() - 1);
                return textFormat();
         }
       }
  [^]                  { yypopstate(); return textFormat(); }
}

<MONO_SECOND_TRY> {
  {MONO} {MONO}? [^\`\n \t] ({WORDNOBACKTICK} | [ \t][\`] | [\`][\p{Letter}\p{Digit}_])* {MONO} {
                         yypushback(yylength() - 1);
                         yypopstate();
                         if(isUnconstrainedStart() && !singlemono && !doublemono) {
                            singlemono = true; return AsciiDocTokenTypes.MONO_START;
                         } else if (singlemono && isUnconstrainedEnd()) {
                            singlemono = false; return AsciiDocTokenTypes.MONO_END;
                         } else {
                            return textFormat();
                         }
                       }
                       // needed advance in case of no second try possible
  [^]                  { yypopstate(); return textFormat(); }
}

<INLINE_URL_NO_DELIMITER> {
  ( ")"? [;:.,] | ")" ) $    { yypushback(yylength()); yypopstate(); }
  ( ")"? [;:.,] ) [^\s\[\]`*_]  { return AsciiDocTokenTypes.URL_LINK; }
  ( ")" ) [^\s\[\];:.,`*_]      { return AsciiDocTokenTypes.URL_LINK; }
  ( ")"? [;:.,] | ")" )      { yypushback(yylength()); yypopstate(); }
  {MONO}               { if (singlemono && !doublemono && isUnconstrainedEnd()) {
                           yypushback(yylength()); yypopstate();
                         } else {
                           return AsciiDocTokenTypes.URL_LINK;
                         }
                       }
  {DOUBLEMONO}         { if (!singlemono && doublemono) {
                           yypushback(yylength()); yypopstate();
                         } else {
                           return AsciiDocTokenTypes.URL_LINK;
                         }
                       }
  {BOLD}               { if (singlebold && !doublebold && isUnconstrainedEnd()) {
                           yypushback(yylength()); yypopstate();
                         } else {
                           return AsciiDocTokenTypes.URL_LINK;
                         }
                       }
  {DOUBLEBOLD}         { if (!singlebold && doublebold) {
                           yypushback(yylength()); yypopstate();
                         } else {
                           return AsciiDocTokenTypes.URL_LINK;
                         }
                       }
  {ITALIC}               { if (singleitalic && !doubleitalic && isUnconstrainedEnd()) {
                           yypushback(yylength()); yypopstate();
                         } else {
                           return AsciiDocTokenTypes.URL_LINK;
                         }
                       }
  {DOUBLEITALIC}         { if (!singleitalic && doubleitalic) {
                           yypushback(yylength()); yypopstate();
                         } else {
                           return AsciiDocTokenTypes.URL_LINK;
                         }
                       }
  [.,] {SPACE}+ $      { yypushback(yylength()); yypopstate(); }
  "["                  { yybegin(LINKTEXT); return AsciiDocTokenTypes.LINKTEXT_START; }
  [\s\[\]]             { yypushback(yylength()); yypopstate(); }
  [^]                  { return AsciiDocTokenTypes.URL_LINK; }
}

<INLINE_URL_IN_BRACKETS> {
  ">" $                { yypopstate(); return AsciiDocTokenTypes.URL_END; }
  ">" [^\s\[\]]* / ">" { return AsciiDocTokenTypes.URL_LINK; }
  ">"                  { return AsciiDocTokenTypes.URL_END; }
  [\s\[\]]             { yypushback(yylength()); yypopstate(); }
  [^]                  { return AsciiDocTokenTypes.URL_LINK; }
}

<INLINE_EMAIL_WITH_PREFIX> {
  "["                  { yybegin(LINKTEXT); return AsciiDocTokenTypes.LINKTEXT_START; }
  [\s\[\]]             { yypushback(yylength()); yypopstate(); }
  [^]                  { return AsciiDocTokenTypes.URL_EMAIL; }
}

<REF, REFTEXT> {
  {REFEND}             { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.REFEND; }
}

<REF> {
  ","                  { yybegin(REFTEXT); return AsciiDocTokenTypes.SEPARATOR; }
  [^]                  { return AsciiDocTokenTypes.REF; }
}

<REFTEXT> {
  [^]                  { return AsciiDocTokenTypes.REFTEXT; }
}

<REFAUTO> {
  [ ,]                 { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.REF; }
  [^]                  { return AsciiDocTokenTypes.REF; }
}

<LINKFILEWITHBLANK>    {
  \s                   { return AsciiDocTokenTypes.LINKFILE; }
}

<LINKFILE, LINKFILEWITHBLANK, LINKANCHOR, LINKURL> {
  {LINKTEXT_START}     { yybegin(LINKTEXT); return AsciiDocTokenTypes.LINKTEXT_START; }
  [ \t]                { yypushback(1); yypopstate(); }
}

<LINKFILE, LINKFILEWITHBLANK> {
  "#"                  { yybegin(LINKANCHOR); return AsciiDocTokenTypes.SEPARATOR; }
  [a-zA-Z]{2,6} "://"        { yybegin(LINKURL); return AsciiDocTokenTypes.URL_LINK; }
  "+++" [a-zA-Z]{2,6} "://" {WORD}+ "+++"    { yybegin(LINKURL); return AsciiDocTokenTypes.URL_LINK; }
  "+++" {WORD}+ "+++"    { return AsciiDocTokenTypes.LINKFILE; }
  "++" [a-zA-Z]{2,6} "://" {WORD}+ "++"    { yybegin(LINKURL); return AsciiDocTokenTypes.URL_LINK; }
  "++" {WORD}+ "++"    { return AsciiDocTokenTypes.LINKFILE; }
  {AUTOCOMPLETE}       { return AsciiDocTokenTypes.LINKFILE; }
  [^]                  { return AsciiDocTokenTypes.LINKFILE; }
}

<LINKURL> {
  [^]                  { return AsciiDocTokenTypes.URL_LINK; }
}

<LINKANCHOR> {
  [^]                  { return AsciiDocTokenTypes.LINKANCHOR; }
}

<LINKTEXT> {
  {LINKEND}            { if (isEscaped()) {
                           return AsciiDocTokenTypes.LINKTEXT;
                         } else {
                           yypopstate(); return AsciiDocTokenTypes.LINKEND;
                         }
                       }
  {SPACE}              { return AsciiDocTokenTypes.WHITE_SPACE; }
  {CONTINUATION} / {SPACE}* "\n" {
                         if (isPrefixedBy(SPACES)) {
                           yypushstate();
                           yybegin(EOL_POP);
                           return AsciiDocTokenTypes.CONTINUATION;
                         } else {
                           return AsciiDocTokenTypes.LINKTEXT;
                         }
                       }
  [^]                  { return AsciiDocTokenTypes.LINKTEXT; }
}

<ATTRIBUTE_REF> {
  {ATTRIBUTE_REF_END}  { yypopstate(); return AsciiDocTokenTypes.ATTRIBUTE_REF_END; }
  [^]                  { return AsciiDocTokenTypes.ATTRIBUTE_REF; }
}

<BLOCKID, BLOCKREFTEXT> {
  {BLOCKIDEND}         {
        yybegin(PREBLOCK);
        yypushstate();
        yybegin(EOL_POP);
        return AsciiDocTokenTypes.BLOCKIDEND;
      }
}

<BLOCKID> {
  ","                  { yybegin(BLOCKREFTEXT); return AsciiDocTokenTypes.SEPARATOR; }
  [^]                  { return AsciiDocTokenTypes.BLOCKID; }
}

<BLOCKREFTEXT> {
  [^]                  { return AsciiDocTokenTypes.BLOCKREFTEXT; }
}

<INLINEID, INLINEREFTEXT> {
  {BLOCKIDEND}         { yypopstate(); return AsciiDocTokenTypes.INLINEIDEND; }
}

<INLINEID> {
  ","                  { yybegin(BLOCKREFTEXT); return AsciiDocTokenTypes.SEPARATOR; }
  [^]                  { return AsciiDocTokenTypes.BLOCKID; }
}

<INLINEREFTEXT> {
  [^]                  { return AsciiDocTokenTypes.BLOCKREFTEXT; }
}

<ANCHORID, ANCHORREFTEXT> {
  {ANCHOREND}         {
        yybegin(PREBLOCK);
        yypushstate();
        yybegin(EOL_POP);
        return AsciiDocTokenTypes.BLOCKIDEND;
      }
}

<ANCHORID> {
  [,.]                 { yybegin(ANCHORREFTEXT); return AsciiDocTokenTypes.SEPARATOR; }
  [^]                  { return AsciiDocTokenTypes.BLOCKID; }
}

<ANCHORREFTEXT> {
  [^]                  { return AsciiDocTokenTypes.BLOCKREFTEXT; }
}

<HEADING> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
}

<DOCTITLE> {
  "\n"                 { yypushback(yylength()); headerLines = 0; yybegin(HEADER); }
}

<DOCTITLE, HEADING> {
  [^]                  { return AsciiDocTokenTypes.HEADING_TOKEN; }
}

<BLOCK_MACRO_ATTRS, BLOCK_ATTRS> {
  "=" / {SPACE}* "\"" ( [^\"\n] | "\\\"" )* "\"" { yypushstate(); yybegin(ATTRS_DOUBLE_QUOTE_START); return AsciiDocTokenTypes.ASSIGNMENT; }
  "=" / {SPACE}* "\'" ( [^\'\n] | "\\\'" )* "\'" { yypushstate(); yybegin(ATTRS_SINGLE_QUOTE_START); return AsciiDocTokenTypes.ASSIGNMENT; }
  "=" / {SPACE}* "\"" { yypushstate(); yybegin(ATTRS_DOUBLE_QUOTE_START_NO_CLOSE); return AsciiDocTokenTypes.ASSIGNMENT; }
  "=" / {SPACE}* "\'" { yypushstate(); yybegin(ATTRS_SINGLE_QUOTE_START_NO_CLOSE); return AsciiDocTokenTypes.ASSIGNMENT; }
  "=" { yypushstate(); yybegin(ATTRS_NO_QUOTE);
      if (isTags) {
        yypushstate();
        yybegin(ATTR_VAL_START);
      }
      return AsciiDocTokenTypes.ASSIGNMENT; }
  ","                  { return AsciiDocTokenTypes.SEPARATOR; }
  {SPACE}              { return AsciiDocTokenTypes.WHITE_SPACE; }
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  "]"                  { yypopstate(); return AsciiDocTokenTypes.ATTRS_END; }
}

<BLOCK_ATTRS> {
  [^\],=\n\t }]+ {
          if ((yycharat(0) != '.' && yycharat(0) != '%') && !yytext().toString().equals("role")) {
            String style = yytext().toString();
            if (style.indexOf(",") != -1) {
              style = style.substring(0, style.indexOf(","));
            }
            setStyle(style);
          }
          return AsciiDocTokenTypes.ATTR_NAME;
        }
}

<BLOCK_MACRO,IFDEF_IFNDEF_ENDIF> {
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
}

<IFDEF_IFNDEF_ENDIF> {
  "["                  { yypushstate(); yybegin(ATTR_PARSEABLE); return AsciiDocTokenTypes.ATTRS_START; }
  [,+]                 { return AsciiDocTokenTypes.SEPARATOR; }
  [^]                  { return AsciiDocTokenTypes.ATTRIBUTE_REF; }
}

<ATTR_PARSEABLE> {
  "]"                  { yypopstate(); return AsciiDocTokenTypes.ATTRS_END; }
  "\n"                 { yypushback(yylength()); yypopstate(); }
  [^]                  { yypushback(yylength()); yypushstate(); yybegin(PREBLOCK); }
}

<BLOCK_MACRO> {
  "["                  { isTags = false; yybegin(BLOCK_MACRO_ATTRS); return AsciiDocTokenTypes.ATTRS_START; }
  [^]                  { return AsciiDocTokenTypes.BLOCK_MACRO_BODY; }
}

<BLOCK_MACRO_ATTRS, BLOCK_ATTRS> {
  "tags" / [ ]* "=" {
          isTags = true;
          return AsciiDocTokenTypes.ATTR_NAME;
        }
  "[" [^\]\n]* "]"     { isTags = false; return AsciiDocTokenTypes.ATTR_NAME; }
  [^]                  { isTags = false; return AsciiDocTokenTypes.ATTR_NAME; }
}

<ATTRS_DOUBLE_QUOTE, ATTRS_SINGLE_QUOTE, ATTRS_NO_QUOTE> {
  [ \t] {if (isTags) { return AsciiDocTokenTypes.WHITE_SPACE; } else { return AsciiDocTokenTypes.ATTR_VALUE; }}
}

<ATTRS_DOUBLE_QUOTE_START> {
  {SPACE} {
      return AsciiDocTokenTypes.WHITE_SPACE;
  }
  "\"" { yybegin(ATTRS_DOUBLE_QUOTE);
        if (isTags) {
          yypushstate();
          yybegin(ATTR_VAL_START);
        }
        return AsciiDocTokenTypes.DOUBLE_QUOTE; }
}

<ATTRS_SINGLE_QUOTE_START> {
  {SPACE} {
      return AsciiDocTokenTypes.WHITE_SPACE;
  }
  "\'" { yybegin(ATTRS_SINGLE_QUOTE);
        if (isTags) {
          yypushstate();
          yybegin(ATTR_VAL_START);
        }
        return AsciiDocTokenTypes.SINGLE_QUOTE; }
}

<ATTRS_DOUBLE_QUOTE> {
  {SPACE} {
      return AsciiDocTokenTypes.WHITE_SPACE;
  }
  "\\\"" { return AsciiDocTokenTypes.ATTR_VALUE; }
  "\"" { yypopstate(); return AsciiDocTokenTypes.DOUBLE_QUOTE; }
}

<ATTRS_SINGLE_QUOTE> {
  {SPACE} {
      return AsciiDocTokenTypes.WHITE_SPACE;
  }
  "\\\'" { return AsciiDocTokenTypes.ATTR_VALUE; }
  "\'" { yypopstate(); return AsciiDocTokenTypes.SINGLE_QUOTE; }
}

<ATTRS_SINGLE_QUOTE,ATTRS_DOUBLE_QUOTE> {
  [,;] { if (isTags) { return AsciiDocTokenTypes.ATTR_LIST_SEP; } else { return AsciiDocTokenTypes.ATTR_VALUE; } }
}

<ATTR_VAL_START> {
  "!" { yypopstate();
        return AsciiDocTokenTypes.ATTR_LIST_OP; }
  {SPACE} { return AsciiDocTokenTypes.WHITE_SPACE; }
  [^] { yypushback(yylength()); yypopstate(); }
}

<ATTRS_NO_QUOTE> {
  [,\]] { yypushback(yylength()); yypopstate(); }
  ";" { if (isTags) { return AsciiDocTokenTypes.ATTR_LIST_SEP; } else { return AsciiDocTokenTypes.ATTR_VALUE; } }
}

<ATTRS_DOUBLE_QUOTE, ATTRS_SINGLE_QUOTE> {
  {INLINE_URL_WITH_DELIMITER} {
        if (isEscaped() || isPrefixedBy(COLONSLASH)) {
          return textFormat();
        } else {
          return AsciiDocTokenTypes.URL_LINK;
        }
  }
}

<ATTRS_DOUBLE_QUOTE, ATTRS_SINGLE_QUOTE, ATTRS_NO_QUOTE> {
  {INLINE_URL_WITH_DELIMITER} {
        if (isEscaped() || isPrefixedBy(COLONSLASH)) {
          return textFormat();
        } else {
          return AsciiDocTokenTypes.URL_LINK;
        }
  }
  "\n" { yypushback(yylength()); yypopstate(); }
  [^] { return AsciiDocTokenTypes.ATTR_VALUE; }
}

<INLINE_MACRO_URL> {
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  "["                  { yypushstate(); yybegin(INLINE_MACRO_ATTRS); return AsciiDocTokenTypes.INLINE_ATTRS_START; }
  "]"                  { yypopstate(); return AsciiDocTokenTypes.INLINE_ATTRS_END; }
  [^]                  { return AsciiDocTokenTypes.URL_LINK; }
}

<INLINE_MACRO, KBD_MACRO> {
  "\n"                 { yypopstate(); yypushback(yylength()); }
  "["                  { yypushstate(); yybegin(yystate() == INLINE_MACRO ? INLINE_MACRO_ATTRS : KBD_MACRO_ATTRS); return AsciiDocTokenTypes.INLINE_ATTRS_START; }
  "]"                  { yypopstate(); return AsciiDocTokenTypes.INLINE_ATTRS_END; }
  "IntellijIdeaRulezzz " / [^\t \n:]* "[" { return AsciiDocTokenTypes.INLINE_MACRO_BODY; }
  "IntellijIdeaRulezzz " { yypopstate(); return AsciiDocTokenTypes.INLINE_MACRO_BODY; }
  [^]                  { return AsciiDocTokenTypes.INLINE_MACRO_BODY; }
}

<ATTRS_DOUBLE_QUOTE_START_NO_CLOSE> {
  {SPACE} {
      return AsciiDocTokenTypes.WHITE_SPACE;
  }
  "\"" {
      yybegin(ATTRS_NO_QUOTE);
      return AsciiDocTokenTypes.DOUBLE_QUOTE;
  }
  [^] {
      yybegin(ATTRS_NO_QUOTE);
      return AsciiDocTokenTypes.DOUBLE_QUOTE;
  }
}

<ATTRS_SINGLE_QUOTE_START_NO_CLOSE> {
  {SPACE} {
      return AsciiDocTokenTypes.WHITE_SPACE;
  }
  "'" {
      yybegin(ATTRS_NO_QUOTE);
      return AsciiDocTokenTypes.SINGLE_QUOTE;
  }
  [^] {
      yybegin(ATTRS_NO_QUOTE);
      return AsciiDocTokenTypes.SINGLE_QUOTE;
  }
}

<INLINE_MACRO_ATTRS> {
  "=" / {SPACE}* "\"" ( [^\"\n] | "\\\"" )* "\"" { yypushstate(); yybegin(ATTRS_DOUBLE_QUOTE_START); return AsciiDocTokenTypes.ASSIGNMENT; }
  "=" / {SPACE}* "\'" ( [^\'\n] | "\\\'" )* "\'" { yypushstate(); yybegin(ATTRS_SINGLE_QUOTE_START); return AsciiDocTokenTypes.ASSIGNMENT; }
  "=" / {SPACE}* "\"" { yypushstate(); yybegin(ATTRS_DOUBLE_QUOTE_START_NO_CLOSE); return AsciiDocTokenTypes.ASSIGNMENT; }
  "=" / {SPACE}* "\'" { yypushstate(); yybegin(ATTRS_SINGLE_QUOTE_START_NO_CLOSE); return AsciiDocTokenTypes.ASSIGNMENT; }
  "=" { yypushstate(); yybegin(ATTRS_NO_QUOTE); return AsciiDocTokenTypes.ASSIGNMENT; }
  "\n" {SPACE}* "\n"   { yypopstate(); yypushback(yylength()); }
  {CONTINUATION} / {SPACE}* "\n" {
                         if (isPrefixedBy(SPACES)) {
                           yypushstate();
                           yybegin(EOL_POP);
                           return AsciiDocTokenTypes.CONTINUATION;
                         } else {
                           return AsciiDocTokenTypes.ATTR_NAME;
                         }
                       }
  "\n"                 { return AsciiDocTokenTypes.LINE_BREAK; }
  "]"                  { yypopstate(); yypushback(yylength()); }
  ","                  { return AsciiDocTokenTypes.SEPARATOR; }
  {SPACE}              { return AsciiDocTokenTypes.WHITE_SPACE; }
  "[" [^\]\n]* "]"     { return AsciiDocTokenTypes.ATTR_NAME; }
  [^]                  { return AsciiDocTokenTypes.ATTR_NAME; }
}

<LISTING_NO_DELIMITER> {
  ^ {SPACE}* "\n" {
        clearStyle();
        yybegin(MULTILINE);
        yypushback(yylength());
      }
  "\n" {
        return AsciiDocTokenTypes.LINE_BREAK;
      }
  [^] {
        return AsciiDocTokenTypes.LISTING_TEXT;
      }
}

<PASSTHROUGH_NO_DELIMITER> {
  ^ {SPACE}* "\n" {
        clearStyle();
        yybegin(MULTILINE);
        yypushback(yylength());
      }
  "\n" {
        return AsciiDocTokenTypes.LINE_BREAK;
      }
  [^] {
        return AsciiDocTokenTypes.PASSTRHOUGH_CONTENT;
      }
}

<LISTING_BLOCK> {
  ^ {LISTING_BLOCK_DELIMITER_END} $ {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yybegin(PREBLOCK);
      yypushstate();
      yybegin(EOL_POP);
      return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER;
    } else {
      return AsciiDocTokenTypes.LISTING_TEXT;
    }
  }
  // duplicating to handle end of file content
  ^ {LISTING_BLOCK_DELIMITER_END} / [^\-\n \t] {
    return AsciiDocTokenTypes.LISTING_TEXT;
  }
  ^ {LISTING_BLOCK_DELIMITER_END} {SPACE}+ "-" {
    return AsciiDocTokenTypes.LISTING_TEXT;
  }
  ^ {LISTING_BLOCK_DELIMITER_END} | {MARKDOWN_LISTING_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yyinitialIfNotInBlock();
      yypushstate();
      yybegin(EOL_POP);
      return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER;
    } else {
      return AsciiDocTokenTypes.LISTING_TEXT;
    }
  }
  "\n"                 { return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.LISTING_TEXT; }
}

<COMMENT_BLOCK> {
  ^ {COMMENT_BLOCK_DELIMITER} $ {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yybegin(EOL_POP);
      return AsciiDocTokenTypes.BLOCK_COMMENT;
    } else {
      return AsciiDocTokenTypes.BLOCK_COMMENT;
    }
  }
  // duplicating to handle end of file content
  ^ {COMMENT_BLOCK_DELIMITER} / [^\/\n \t] { return AsciiDocTokenTypes.BLOCK_COMMENT; }
  ^ {COMMENT_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yyinitialIfNotInBlock();
      return AsciiDocTokenTypes.BLOCK_COMMENT;
    } else {
      return AsciiDocTokenTypes.BLOCK_COMMENT;
    }
  }
  "\n"                 { return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.BLOCK_COMMENT; }
}

<PASSTRHOUGH_INLINE_CONSTRAINED, PASSTRHOUGH_INLINE, PASSTRHOUGH_INLINE_UNCONSTRAINED> {
  // blank lines within pre block don't have an effect
  ^ {SPACE}* "\n"           { yypushback(yylength()); yypopstate(); }
}

<PASSTRHOUGH_INLINE> {
  {PASSTRHOUGH_INLINE} { yypopstate(); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_END; }
  [^]                  { return AsciiDocTokenTypes.PASSTRHOUGH_CONTENT; }
}

<PASSTRHOUGH_INLINE_CONSTRAINED> {
  "++" { yypopstate(); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_END; }
  [^]                  { return AsciiDocTokenTypes.PASSTRHOUGH_CONTENT; }
}

<PASSTRHOUGH_INLINE_UNCONSTRAINED> {
  "+" { if (isUnconstrainedEnd() && !isPrefixedBy("+".toCharArray())) { yypopstate(); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_END; }
        return AsciiDocTokenTypes.PASSTRHOUGH_CONTENT; }
  [^]  { return AsciiDocTokenTypes.PASSTRHOUGH_CONTENT; }
}

<PASSTRHOUGH_BLOCK> {
  ^ {PASSTRHOUGH_BLOCK_DELIMITER} $ {
      if (yytext().toString().trim().length() == blockDelimiterLength) {
        clearStyle();
        yybegin(MULTILINE);
        return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER;
      } else {
        return AsciiDocTokenTypes.PASSTRHOUGH_CONTENT;
      }
    }
  // duplicating to handle end of file content
  ^ {PASSTRHOUGH_BLOCK_DELIMITER} / [^\+\n \t] { return AsciiDocTokenTypes.PASSTRHOUGH_CONTENT; }
  ^ {PASSTRHOUGH_BLOCK_DELIMITER} {
      if (yytext().toString().trim().length() == blockDelimiterLength) {
        clearStyle();
        yybegin(MULTILINE);
        return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER;
      } else {
        return AsciiDocTokenTypes.PASSTRHOUGH_CONTENT;
      }
    }
  "\n"                 { return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.PASSTRHOUGH_CONTENT; }
}

<LITERAL_BLOCK> {
  ^ {LITERAL_BLOCK_DELIMITER} $ {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      clearStyle();
      yybegin(MULTILINE);
      return AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER;
    } else {
      return AsciiDocTokenTypes.LITERAL_BLOCK;
    }
  }
  // duplicating to handle end of file content
  ^ {LITERAL_BLOCK_DELIMITER} / [^\.\n \t] { return AsciiDocTokenTypes.LITERAL_BLOCK; }
  ^ {LITERAL_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      clearStyle();
      yybegin(MULTILINE);
      return AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER;
    } else {
      return AsciiDocTokenTypes.LITERAL_BLOCK;
    }
  }
  "\n"                 { return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.LITERAL_BLOCK; }
}

// include is the only allowed block macro in these types of block
<LITERAL_BLOCK, LISTING_BLOCK, PASSTRHOUGH_BLOCK, LISTING_NO_DELIMITER, SINGLELINE, HEADER, LIST> {
  ^ "include::" / [^\[\n]* "[" [^\]\n]* "]" {SPACE}* \n { yypushstate(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  ^ "include::" / [^\[\n]* {AUTOCOMPLETE} { yypushstate(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
}

<FRONTMATTER> {
   ^ "---" [ \t]* $ {
      yypushback(yylength() - 3);
      yybegin(EOL_POP);
      return AsciiDocTokenTypes.FRONTMATTER_DELIMITER;
   }
  ^ "---" / [^\.\n \t] { return AsciiDocTokenTypes.FRONTMATTER; }
  ^ "---"  { yybegin(EOL_POP); return AsciiDocTokenTypes.FRONTMATTER_DELIMITER; }
  "\n"                 { return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.FRONTMATTER; }
}

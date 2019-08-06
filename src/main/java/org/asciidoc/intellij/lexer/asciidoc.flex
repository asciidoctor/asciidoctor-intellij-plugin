package org.asciidoc.intellij.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import java.util.Stack;

%%

%class _AsciiDocLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

%{
  private int blockDelimiterLength;
  private boolean singlebold = false;
  private boolean doublebold = false;
  private boolean singleitalic = false;
  private boolean doubleitalic = false;
  private boolean singlemono = false;
  private boolean doublemono = false;
  private boolean typographicquote = false;
  private String style = null;
  private int headerLines = 0;

  private Stack<Integer> stateStack = new Stack<>();

  private Stack<String> blockStack = new Stack<>();

  private boolean isPrefixedBy(String prefix) {
    if(getTokenStart() > 0) {
      char c = zzBuffer.charAt(getTokenStart() -1);
      for (char p : prefix.toCharArray()) {
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
      if (Character.isAlphabetic(c) || c == '_') {
        return false;
      }
    }
    return true;
  }

  private boolean isUnconstrainedStart() {
    if(getTokenStart() > 0) {
      char c = zzBuffer.charAt(getTokenStart() -1);
      if (Character.isAlphabetic(c) || c == '_' || c == ':' || c == ';' || c == '\\') {
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
    if(blockStack.size() > 0 && blockStack.peek().equals("nodel")) {
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
  }
  private IElementType textFormat() {
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

  private void yyinitialIfNotInBlock() {
    if (blockStack.size() == 0 && style == null) {
      yybegin(YYINITIAL);
    } else {
      yybegin(MULTILINE);
    }
  }
%}

SPACE = [\ \t]
NON_SPACE = [^\n]
LINE_COMMENT="//"[^\n]*
COMMENT_BLOCK_DELIMITER = "////" "/"* {SPACE}*
PASSTRHOUGH_BLOCK_DELIMITER = "++++" "+"* {SPACE}*
LISTING_BLOCK_DELIMITER = "----" "-"* {SPACE}*
MARKDOWN_LISTING_BLOCK_DELIMITER = "```" {SPACE}*
EXAMPLE_BLOCK_DELIMITER = "====" "="* {SPACE}*
SIDEBAR_BLOCK_DELIMITER = "****" "*"* {SPACE}*
QUOTE_BLOCK_DELIMITER = "____" "_"* {SPACE}*
LITERAL_BLOCK_DELIMITER = "...." "."* {SPACE}*
TABLE_BLOCK_DELIMITER = "|===" "="* {SPACE}*
OPEN_BLOCK_DELIMITER = "--" {SPACE}*
CONTINUATION = "+"
HEADING_START = "="{1,6} {SPACE}+
HEADING_START_MARKDOWN = "#"{1,6} {SPACE}+
// starting at the start of the line, but not with a dot
// next line follwoing with only header marks
HEADING_OLDSTYLE = {SPACE}* [^ _+\-#=~.\n\t\[].* "\n" [-=~\^+]+ {SPACE}* "\n"
BLOCK_MACRO_START = [a-zA-Z0-9_]+"::"
INLINE_MACRO_START = [a-zA-Z0-9_]+":"
TITLE_START = "."
AUTOCOMPLETE = "IntellijIdeaRulezzz " // CompletionUtilCore.DUMMY_IDENTIFIER
BLOCK_ATTRS_START = "["
STRING = {NON_SPACE}+ \n? // something that doesn't have an empty line
// something with a non-blank at the end, might contain a line break, but only if it doesn't separate the block
WORD = {SPACE}* [^\n]* {SPACE}* \n {SPACE}* [^\ \t\n] | {SPACE}* [^\n]*[^\ \t\n]
BOLD = "*"
BULLET = ("*"+|"-"+)
ENUMERATION = ([0-9]*|[a-zA-Z]?)"."+
CALLOUT = "<" [0-9] ">"
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
SINGLE_QUOTE = "'"
DOUBLE_QUOTE = "\""
TYPOGRAPHIC_DOUBLE_QUOTE_START = "\"`"
TYPOGRAPHIC_DOUBLE_QUOTE_END = "`\""
TYPOGRAPHIC_SINGLE_QUOTE_START = "'`"
TYPOGRAPHIC_SINGLE_QUOTE_END = "`'"
ANCHORSTART = "[#"
ANCHOREND = "]"
LINKSTART = "link:"
LINKTEXT_START = "["
INLINE_URL_NO_DELIMITER = (https?|file|ftp|irc): "//" [^\s\[\]<]*([^\s.,;\[\]<\)])
INLINE_URL_WITH_DELIMITER = (https?|file|ftp|irc): "//" [^\s\[\]<]*([^\s\[\]])
INLINE_EMAIL_NO_DELIMITER = [[:letter:][:digit:]_](&amp;|[[:letter:][:digit:]_\-.%+])*@[[:letter:][:digit:]][[:letter:][:digit:]_\-.]*\.[a-zA-Z]{2,5}
LINKEND = "]"
ATTRIBUTE_NAME_START = ":"
ATTRIBUTE_NAME = [a-zA-Z0-9_]+ [a-zA-Z0-9_-]*
ATTRIBUTE_NAME_END = ":"
ATTRIBUTE_REF_START = "{"
ATTRIBUTE_REF_END = "}"
END_OF_SENTENCE = [\.?!:] | (" " [?!:]) // French: "marks with two elements require a space before them in"
HARD_BREAK = {SPACE} "+" {SPACE}* "\n"
DESCRIPTION = [^\n]+ {SPACE}* (":"{2,4} | ";;")
ADMONITION = ("NOTE" | "TIP" | "IMPORTANT" | "CAUTION" | "WARNING" ) ":"

%state MULTILINE
%state HEADER
%state PREBLOCK
%state STARTBLOCK
%state DELIMITER
%state SINGLELINE
%state AFTER_SPACE
%state INSIDE_LINE
%state MONO_SECOND_TRY
%state REF
%state REFTEXT
%state REFAUTO
%state BLOCKID
%state BLOCKREFTEXT
%state HEADING
%state DOCTITLE
%state ANCHORID
%state ANCHORREFTEXT

%state INLINE_URL_NO_DELIMITER
%state INLINE_URL_IN_BRACKETS
%state INLINE_URL_WITH_PREFIX
%state INLINE_EMAIL_WITH_PREFIX

%state LISTING_BLOCK
%state LISTING_NO_DELIMITER
%state LISTING_NO_DELIMITER

%state COMMENT_BLOCK
%state EXAMPLE_BLOCK
%state SIDEBAR_BLOCK
%state QUOTE_BLOCK
%state LITERAL_BLOCK
%state PASSTRHOUGH_BLOCK

%state PASSTRHOUGH_INLINE
%state PASSTHROUGH_NO_DELIMITER
%state PASSTHROUGH_NO_DELIMITER

%state BLOCK_MACRO
%state BLOCK_MACRO_ATTRS
%state BLOCK_ATTRS

%state INLINE_MACRO
%state INLINE_MACRO_ATTRS
%state INLINE_ATTRS

%state TITLE

%state ATTRIBUTE_NAME
%state ATTRIBUTE_VAL
%state ATTRIBUTE_REF_START
%state ATTRIBUTE_REF

%state LINKSTART
%state LINKFILE
%state LINKURL
%state LINKANCHOR
%state LINKTEXT
%state LINKEND

%%

// IntelliJ might do partial parsing from any YYINITIAL inside a document
// therefore only return here is no other state (i.e. bold) needs to be preserved
<YYINITIAL> {
  [^]                  { yypushback(yylength()); clearStyle(); resetFormatting(); blockStack.clear(); stateStack.clear(); yybegin(MULTILINE); }
}

<MULTILINE> {
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
             // only plus signs are never a heading but a continuation (single plus) or something else
             && !heading.matches("^\\+*$")
             // only minus signs are never a heading but block (double minus), a horizontal rule (triple minus) or something else
             && !heading.matches("^-*$")) {
            // push back the second newline of the pattern
            yypushback(1);
            resetFormatting();
            if (underlining.startsWith("=")) {
              headerLines = 0;
              yybegin(HEADER);
            }
            return AsciiDocTokenTypes.HEADING;
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
  {ATTRIBUTE_NAME_END} { yybegin(ATTRIBUTE_VAL); return AsciiDocTokenTypes.ATTRIBUTE_NAME_END; }
  {AUTOCOMPLETE} | {ATTRIBUTE_NAME} { return AsciiDocTokenTypes.ATTRIBUTE_NAME; }
  "\n"               { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                { yypushback(yylength()); yypopstate(); }
}

<ATTRIBUTE_VAL> {
  {ATTRIBUTE_REF_START} / {ATTRIBUTE_NAME} {ATTRIBUTE_REF_END} {
                         if (!isEscaped()) {
                           yypushstate(); yybegin(ATTRIBUTE_REF); return AsciiDocTokenTypes.ATTRIBUTE_REF_START;
                         } else {
                           return AsciiDocTokenTypes.ATTRIBUTE_VAL;
                         }
                       }
  /*Value continue on the next line if the line is ended by a space followed by a backslash*/
  {SPACE} "\\" {SPACE}* "\n" { return AsciiDocTokenTypes.ATTRIBUTE_VAL; }
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.ATTRIBUTE_VAL; }
}

// everything that will not render after a [source] as literal text
// especially: titles, block attributes, block ID, etc.
<PREBLOCK> {
  ^ [ \t]+ / ({BULLET} {SPACE}+ {STRING} | {ENUMERATION} {SPACE}+ {STRING} | {DESCRIPTION} {SPACE}+ {STRING} | {DESCRIPTION} {SPACE}* "\n" )  {
        yypushback(yylength()); yybegin(SINGLELINE);
      }
  ^ [ \t]+ / [^ \t\n] {
        if (style == null) {
          yypushback(yylength()); yybegin(LISTING_NO_DELIMITER);
        } else {
          yypushback(yylength()); yybegin(STARTBLOCK);
        }
      }
  {SPACE}* "\n"           { resetFormatting(); yybegin(MULTILINE); return AsciiDocTokenTypes.LINE_BREAK; } // blank lines within pre block don't have an effect
  ^ {TITLE_START} / [^\. ] { resetFormatting(); yybegin(TITLE); return AsciiDocTokenTypes.TITLE; }
}

<HEADER, PREBLOCK> {
  ^ {ATTRIBUTE_NAME_START} / {AUTOCOMPLETE}? {ATTRIBUTE_NAME} {ATTRIBUTE_NAME_END} {
        if (!isEscaped()) {
          yypushstate();
          yybegin(ATTRIBUTE_NAME);
          return AsciiDocTokenTypes.ATTRIBUTE_NAME_START;
        } else {
          yypushback(yylength()); yybegin(STARTBLOCK);
        }
      }
  ^ {ATTRIBUTE_NAME_START} / [^:\n \t]* {AUTOCOMPLETE} {
    if (!isEscaped()) {
      yypushstate();
      yybegin(ATTRIBUTE_NAME); return AsciiDocTokenTypes.ATTRIBUTE_NAME_START;
    } else {
      return textFormat();
    }
  }
}

<HEADER> {
  ^{SPACE}* "\n" {
        yypushback(yylength());
        yybegin(PREBLOCK);
  }
  "\n" {
        ++ headerLines;
        if (headerLines >= 3) {
          yybegin(PREBLOCK);
        }
        return AsciiDocTokenTypes.LINE_BREAK;
  }
  [^] {
        return AsciiDocTokenTypes.HEADER;
  }
}

<DELIMITER, PREBLOCK> {
  ^ {PAGEBREAK} $ { resetFormatting(); yybegin(PREBLOCK); return AsciiDocTokenTypes.PAGEBREAK; }
  ^ {HORIZONTALRULE} $ { resetFormatting(); yybegin(PREBLOCK); return AsciiDocTokenTypes.HORIZONTALRULE; }
  ^ {BLOCK_MACRO_START} / [^ \[\n] [^\[\n]* { yypushstate(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  // endif allows the body to be empty, special case...
  ^ "endif::" / [^ \n] { yypushstate(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  ^ {BLOCK_MACRO_START} / [^ \[\n]? [^\[\n]* {AUTOCOMPLETE} { yypushstate(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  {BLOCK_ATTRS_START} / [^\[] { yybegin(MULTILINE); yypushstate(); clearStyle(); yybegin(BLOCK_ATTRS); return AsciiDocTokenTypes.BLOCK_ATTRS_START; }
  {ANCHORSTART} / [^\]\n]+ {ANCHOREND} { resetFormatting(); yybegin(ANCHORID); return AsciiDocTokenTypes.BLOCKIDSTART; }
  {BLOCKIDSTART} / [^\]\n]+ {BLOCKIDEND} {
                         if (!isEscaped()) {
                           yybegin(BLOCKID); return AsciiDocTokenTypes.BLOCKIDSTART;
                         } else {
                           yypushback(1);
                           return AsciiDocTokenTypes.LBRACKET;
                         }
                       }
  // triple rules to handle EOF
  ^ {LISTING_BLOCK_DELIMITER} $ { clearStyle(); resetFormatting(); yybegin(LISTING_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER; }
  ^ {LISTING_BLOCK_DELIMITER} / [^\-\n \t] { yypushback(yylength()); yybegin(STARTBLOCK);  }
  ^ {LISTING_BLOCK_DELIMITER} | {MARKDOWN_LISTING_BLOCK_DELIMITER} { clearStyle(); resetFormatting(); yybegin(LISTING_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER; }

  ^ {PASSTRHOUGH_BLOCK_DELIMITER} $ { clearStyle(); resetFormatting(); yybegin(PASSTRHOUGH_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER; }
  ^ {PASSTRHOUGH_BLOCK_DELIMITER} / [^\+\n \t] { yypushback(yylength()); yybegin(STARTBLOCK);  }
  ^ {PASSTRHOUGH_BLOCK_DELIMITER} { clearStyle(); resetFormatting(); yybegin(PASSTRHOUGH_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER; }

  ^ {HEADING_START} | {HEADING_START_MARKDOWN} / {NON_SPACE} { if (blockStack.size() == 0) {
                              int level = 0;
                              CharSequence prefix = yytext();
                              while (prefix.length() > level && (prefix.charAt(level) == '#' || prefix.charAt(level) == '=')) {
                                ++ level;
                              }
                              if (level == 1) {
                                yybegin(DOCTITLE);
                              } else {
                                yybegin(HEADING);
                              }
                              clearStyle(); resetFormatting(); return AsciiDocTokenTypes.HEADING;
                            }
                            yypushback(yylength()); yybegin(STARTBLOCK);
                          }

  ^ ({EXAMPLE_BLOCK_DELIMITER} | {QUOTE_BLOCK_DELIMITER} | {SIDEBAR_BLOCK_DELIMITER} | {TABLE_BLOCK_DELIMITER} | {OPEN_BLOCK_DELIMITER}) $ {
                            clearStyle();
                            resetFormatting();
                            String delimiter = yytext().toString().trim();
                            if(blockStack.contains(delimiter)) {
                              while (blockStack.contains(delimiter)) {
                                blockStack.pop();
                              }
                            } else {
                              blockStack.push(delimiter);
                            }
                            yybegin(PREBLOCK);
                            return AsciiDocTokenTypes.BLOCK_DELIMITER;
                          }
  ^ {QUOTE_BLOCK_DELIMITER} / [^\_\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); /* QUOTE_BLOCK_DELIMITER */ }
  ^ {SIDEBAR_BLOCK_DELIMITER} / [^\*\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); /* SIDEBAR_BLOCK_DELIMITER */ }
  ^ {TABLE_BLOCK_DELIMITER} / [^\=\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); /* TABLE_BLOCK_DELIMITER */ }
  ^ {OPEN_BLOCK_DELIMITER} / [^\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); /* OPEN_BLOCK_DELIMITER */ }
  ^ {EXAMPLE_BLOCK_DELIMITER} / [^\=\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); /* EXAMPLE_BLOCK_DELIMITER */ }
  ^ ({EXAMPLE_BLOCK_DELIMITER} | {QUOTE_BLOCK_DELIMITER} | {SIDEBAR_BLOCK_DELIMITER} | {TABLE_BLOCK_DELIMITER} | {OPEN_BLOCK_DELIMITER})  {
                            clearStyle();
                            resetFormatting();
                            String delimiter = yytext().toString().trim();
                            if(blockStack.contains(delimiter)) {
                              while (blockStack.contains(delimiter)) {
                                blockStack.pop();
                              }
                            } else {
                              blockStack.push(delimiter);
                            }
                            yybegin(PREBLOCK);
                            return AsciiDocTokenTypes.BLOCK_DELIMITER;
                          }

  ^ {LITERAL_BLOCK_DELIMITER} $ { clearStyle(); resetFormatting(); yybegin(LITERAL_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER; }
  ^ {LITERAL_BLOCK_DELIMITER} / [^\n \t] { yypushback(yylength()); yybegin(STARTBLOCK); }
  ^ {LITERAL_BLOCK_DELIMITER} { clearStyle(); resetFormatting(); yybegin(LITERAL_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER; }
}

<DELIMITER> {
  {SPACE}* "\n"           { yypushback(yylength()); yybegin(PREBLOCK); } // blank lines don't have an effect
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
          blockStack.push("nodel");
          yypushback(yylength()); yybegin(SINGLELINE);
        }
        clearStyle();
      }
}

<SINGLELINE, LISTING_NO_DELIMITER, LITERAL_BLOCK, QUOTE_BLOCK, EXAMPLE_BLOCK, SIDEBAR_BLOCK> {
  // this will only terminate any open blocks when they appear even in no-delimiter blocks
  ^ ({EXAMPLE_BLOCK_DELIMITER} | {QUOTE_BLOCK_DELIMITER} | {SIDEBAR_BLOCK_DELIMITER} | {TABLE_BLOCK_DELIMITER} | {OPEN_BLOCK_DELIMITER}) $ {
                            String delimiter = yytext().toString().trim();
                            if(blockStack.contains(delimiter)) {
                              while (blockStack.contains(delimiter)) {
                                blockStack.pop();
                              }
                              yybegin(PREBLOCK);
                              return AsciiDocTokenTypes.BLOCK_DELIMITER;
                            } else {
                              yybegin(INSIDE_LINE);
                              return textFormat();
                            }
                          }
}

<SINGLELINE> {
  "[" [^\]\n]+ "]" / "#" { return textFormat(); } // attribute, not handled yet
  ^ {CALLOUT} / {SPACE}+ {STRING} { resetFormatting(); yybegin(INSIDE_LINE); return AsciiDocTokenTypes.CALLOUT; }
  ^ {ADMONITION} / {SPACE}+ {STRING} { resetFormatting(); yybegin(INSIDE_LINE); return AsciiDocTokenTypes.ADMONITION; }
  /* a blank line, it separates blocks. Don't return YYINITIAL here, as writing on a blank line might change the meaning
  of the previous blocks combined (for example there is now an italic formatting spanning the two combined blocks) */
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
  [ \t]+               { yybegin(AFTER_SPACE);
                         if (singlemono || doublemono) {
                           return AsciiDocTokenTypes.WHITE_SPACE_MONO;
                         } else {
                           return AsciiDocTokenTypes.WHITE_SPACE;
                         }
                       }
  {CONTINUATION} / {SPACE}* "\n" {
                         yybegin(DELIMITER);
                         return AsciiDocTokenTypes.CONTINUATION;
                       }
  [^]                  { yypushback(yylength()); yybegin(AFTER_SPACE); }
}

<PREBLOCK, SINGLELINE, PASSTHROUGH_NO_DELIMITER> {
  {LINE_COMMENT} {
    return AsciiDocTokenTypes.LINE_COMMENT;
  }
}

<PREBLOCK, SINGLELINE, DELIMITER> {
  {COMMENT_BLOCK_DELIMITER} $ { clearStyle(); resetFormatting(); yybegin(COMMENT_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.BLOCK_COMMENT; }
  {COMMENT_BLOCK_DELIMITER} / [^\/\n \t] { yypushback(yylength()); yybegin(STARTBLOCK);  }
  {COMMENT_BLOCK_DELIMITER} { clearStyle(); resetFormatting(); yybegin(COMMENT_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.BLOCK_COMMENT; }
}

<AFTER_SPACE> {
  {BULLET} / {SPACE}+ {STRING} { resetFormatting(); yybegin(INSIDE_LINE); return AsciiDocTokenTypes.BULLET; }
  {ENUMERATION} / {SPACE}+ {STRING} { resetFormatting(); yybegin(INSIDE_LINE); return AsciiDocTokenTypes.ENUMERATION; }
  {DESCRIPTION} / {SPACE}+ {STRING} { resetFormatting(); yybegin(INSIDE_LINE); return AsciiDocTokenTypes.DESCRIPTION; }
  {DESCRIPTION} / {SPACE}* "\n" { resetFormatting(); yybegin(INSIDE_LINE); return AsciiDocTokenTypes.DESCRIPTION; }
  [^]                  { yypushback(yylength()); yybegin(INSIDE_LINE); }
}

<INSIDE_LINE> {
  "\n"                 { if (isNoDel()) {
                           yybegin(SINGLELINE);
                         } else {
                           yybegin(DELIMITER);
                         }
                         return AsciiDocTokenTypes.LINE_BREAK; }
  {HARD_BREAK}
                       { return AsciiDocTokenTypes.HARD_BREAK; }
  // exceptions to END_OF_SENTENCE
  [:letter:] "." " "? [:letter:] "." { return textFormat(); } // i.e., e.g., ...
  "Dr." | "Prof." | "Ing." / {SPACE}* [^ \t\n] { return textFormat(); } // title inside a line as text if inside of a line
  [A-Z] "." / {SPACE}* [^ \t\n] { return textFormat(); } // initials inside a line as text if inside of a line
  ".." "."* / {SPACE}* [^ \t\n] { return textFormat(); } // avoid end of sentence for "..." if inside of a line
  {END_OF_SENTENCE} / {SPACE} (!:uppercase:)* :lowercase: // standard text if followed by lower case character
                       { return textFormat(); }
  {END_OF_SENTENCE} / {SPACE}* \n // end of sentence at end of line
                       { if (!doublemono && !singlemono) {
                           return AsciiDocTokenTypes.END_OF_SENTENCE;
                         } else {
                           return textFormat();
                         }
                       }
  {END_OF_SENTENCE} / {SPACE} // end of sentence within a line, needs to be unconstrained
                       { if (!doublemono && !singlemono && isUnconstrainedEnd()) {
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
  {DOUBLEBOLD} / [^\*] {STRING}* {DOUBLEBOLD} { if(!singlebold) {
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
  {BOLD} {BOLD}? / [^\*\n \t] {WORD}* {BOLD} { if(isUnconstrainedStart() && !singlebold && !doublebold) {
                            if (yylength() == 2) {
                              yypushback(1);
                            }
                            singlebold = true; return AsciiDocTokenTypes.BOLD_START;
                         } else if (singlebold && isUnconstrainedEnd()) {
                            singlebold = false; return AsciiDocTokenTypes.BOLD_END;
                         } else {
                            return textFormat();
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
  // start something with ** only if it closes within the same block
  {DOUBLEITALIC} / [^\_] {STRING}* {DOUBLEITALIC} { if(!singleitalic) {
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
  {ITALIC} {ITALIC}? / [^\_\n \t] {WORD}* {ITALIC} { if(isUnconstrainedStart() && !singleitalic && !doubleitalic) {
                            if (yylength() == 2) {
                              yypushback(1);
                            }
                            singleitalic = true; return AsciiDocTokenTypes.ITALIC_START;
                         } else if (singleitalic && isUnconstrainedEnd()) {
                            singleitalic = false; return AsciiDocTokenTypes.ITALIC_END;
                         } else {
                            return textFormat();
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
  {DOUBLEMONO} / [^\`] {STRING}* {DOUBLEMONO} { if(!singlemono) {
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
                           yybegin(MONO_SECOND_TRY);
                         }
                       }
  // ITALIC END
  {LPAREN}             { return AsciiDocTokenTypes.LPAREN; }
  {RPAREN}             { return AsciiDocTokenTypes.RPAREN; }
  {LBRACKET}           { return AsciiDocTokenTypes.LBRACKET; }
  {RBRACKET}           { return AsciiDocTokenTypes.RBRACKET; }
  {REFSTART} / [^>\n]+ {REFEND} {
                         if (!isEscaped()) {
                           yybegin(REF); return AsciiDocTokenTypes.REFSTART;
                         } else {
                           yypushback(1);
                           return AsciiDocTokenTypes.LT;
                         }
                       }
  // when typing a reference, it will not be complete due to the missing matching closing ref
  // therefore second variante for incomplete REF that will only be active during autocomplete
  {REFSTART} / [^>\n ]* {AUTOCOMPLETE} {
                         if (!isEscaped()) {
                           yybegin(REFAUTO); return AsciiDocTokenTypes.REFSTART;
                         } else {
                           yypushback(1);
                           return AsciiDocTokenTypes.LT;
                         }
                        }
  {ATTRIBUTE_REF_START} / {ATTRIBUTE_NAME} {ATTRIBUTE_REF_END} {
                         if (!isEscaped()) {
                           yypushstate(); yybegin(ATTRIBUTE_REF); return AsciiDocTokenTypes.ATTRIBUTE_REF_START;
                         } else {
                           return textFormat();
                         }
                       }
  (->|=>|<-|<=)        { return textFormat(); } // avoid errors to be recognized as LT/GT
  {LT}                 { return AsciiDocTokenTypes.LT; }
  {GT}                 { return AsciiDocTokenTypes.GT; }
  {SINGLE_QUOTE}       { if (isUnconstrainedStart() || isUnconstrainedEnd()) {
                           return AsciiDocTokenTypes.SINGLE_QUOTE;
                         } else {
                           return textFormat();
                         }
                       }
  {DOUBLE_QUOTE}       { return AsciiDocTokenTypes.DOUBLE_QUOTE; }
  {TYPOGRAPHIC_DOUBLE_QUOTE_START} / [^\*\n \t] {WORD}* {TYPOGRAPHIC_DOUBLE_QUOTE_END} {
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
                             yybegin(MONO_SECOND_TRY);
                           }
                         }
  {TYPOGRAPHIC_SINGLE_QUOTE_START} / [^\*\n \t] {WORD}* {TYPOGRAPHIC_SINGLE_QUOTE_END} {
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
                             yybegin(MONO_SECOND_TRY);
                           }
                         }
  {INLINE_URL_NO_DELIMITER} {
        if (isEscaped() || isPrefixedBy(":/")) {
          return textFormat();
        } else {
          yypushstate();
          yypushback(yylength());
          yybegin(INLINE_URL_NO_DELIMITER);
        }
  }
  {INLINE_EMAIL_NO_DELIMITER} {
        if (isEscaped() || isPrefixedBy(":/")) {
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
  {LINKSTART} / [^\[\n \t]* ( {AUTOCOMPLETE} | {AUTOCOMPLETE}? {LINKTEXT_START} [^\]\n]* {LINKEND}) {
                         if (!isEscaped()) {
                           yypushstate(); yybegin(LINKFILE); return AsciiDocTokenTypes.LINKSTART;
                         } else {
                           return textFormat();
                         }
                       }
  {INLINE_MACRO_START} / ([^ \[\n\"`:/] [^\[\n\"`:]* | "") ({AUTOCOMPLETE} | {AUTOCOMPLETE}? "[" [^\]\n]* "]") {
        if (!isEscaped()) {
          yypushstate();
          yybegin(INLINE_MACRO);
          return AsciiDocTokenTypes.INLINE_MACRO_ID;
        } else {
          return textFormat();
        }
      }
  {ATTRIBUTE_REF_START} / [^}\n ]* {AUTOCOMPLETE} {
                         if (!isEscaped()) {
                           yypushstate(); yybegin(ATTRIBUTE_REF); return AsciiDocTokenTypes.ATTRIBUTE_REF_START;
                         } else {
                           return textFormat();
                         }
                       }
  {PASSTRHOUGH_INLINE} / {STRING}* {PASSTRHOUGH_INLINE} {
                           yybegin(PASSTRHOUGH_INLINE); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_START;
                         }
  [^]                  { return textFormat(); }
}

<MONO_SECOND_TRY> {
  {MONO} {MONO}? / [^\`\n \t] {WORD}* {MONO} {
                         yybegin(INSIDE_LINE);
                         if(isUnconstrainedStart() && !singlemono && !doublemono) {
                            if (yylength() == 2) {
                              yypushback(1);
                            }
                            singlemono = true; return AsciiDocTokenTypes.MONO_START;
                         } else if (singlemono && isUnconstrainedEnd()) {
                            singlemono = false; return AsciiDocTokenTypes.MONO_END;
                         } else {
                            return textFormat();
                         }
                       }
  [^]                  { yybegin(INSIDE_LINE); return textFormat(); }
}

<MONO_SECOND_TRY> {
                       // needed advance in case of no second try possible
  [^]                  { yybegin(INSIDE_LINE); return textFormat(); }
}

<INLINE_URL_NO_DELIMITER> {
  ( ")"? [;:.,] | ")" ) $    { yypushback(yylength()); yypopstate(); }
  ( ")"? [;:.,] ) [^\s\[\]]  { return AsciiDocTokenTypes.URL_LINK; }
  ( ")" ) [^\s\[\];:.,]      { return AsciiDocTokenTypes.URL_LINK; }
  ( ")"? [;:.,] | ")" )      { yypushback(yylength()); yypopstate(); }

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

<INLINE_URL_WITH_PREFIX> {
  "["                  { yybegin(LINKTEXT); return AsciiDocTokenTypes.LINKTEXT_START; }
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
  "#"                  { return AsciiDocTokenTypes.SEPARATOR; }
  [^#>\n]+ / "#"       { return AsciiDocTokenTypes.REFFILE; }
  [^]                  { return AsciiDocTokenTypes.REF; }
}

<REFTEXT> {
  [^]                  { return AsciiDocTokenTypes.REFTEXT; }
}

<REFAUTO> {
  [ ,]                 { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.REF; }
  [^]                  { return AsciiDocTokenTypes.REF; }
}

<LINKFILE, LINKANCHOR, LINKURL> {
  {LINKTEXT_START}     { yybegin(LINKTEXT); return AsciiDocTokenTypes.LINKTEXT_START; }
  [ \t]                { yypushback(1); yypopstate(); }
}

<LINKFILE> {
  "#"                  { yybegin(LINKANCHOR); return AsciiDocTokenTypes.SEPARATOR; }
  [a-zA-Z]+ ":"         { yybegin(LINKURL); return AsciiDocTokenTypes.URL_LINK; }
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
  {LINKEND}            { yypopstate(); return AsciiDocTokenTypes.LINKEND; }
  [^]                  { return AsciiDocTokenTypes.LINKTEXT; }
}

<ATTRIBUTE_REF_START, ATTRIBUTE_REF> {
  {ATTRIBUTE_REF_END}  { yypopstate(); return AsciiDocTokenTypes.ATTRIBUTE_REF_END; }
}

<ATTRIBUTE_REF> {
  [^]                  { return AsciiDocTokenTypes.ATTRIBUTE_REF; }
}

<BLOCKID, BLOCKREFTEXT> {
  {BLOCKIDEND}         { yybegin(PREBLOCK); return AsciiDocTokenTypes.BLOCKIDEND; }
}

<BLOCKID> {
  ","                  { yybegin(BLOCKREFTEXT); return AsciiDocTokenTypes.SEPARATOR; }
  [^]                  { return AsciiDocTokenTypes.BLOCKID; }
}

<BLOCKREFTEXT> {
  [^]                  { return AsciiDocTokenTypes.BLOCKREFTEXT; }
}

<ANCHORID, ANCHORREFTEXT> {
  {ANCHOREND}         { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.BLOCKIDEND; }
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
  [^]                  { return AsciiDocTokenTypes.HEADING; }
}

<TITLE> {
  "\n"                 { yyinitialIfNotInBlock(); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.TITLE; }
}

<BLOCK_ATTRS> {
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  "]"                  { return AsciiDocTokenTypes.BLOCK_ATTRS_END; }
  ","                  { return AsciiDocTokenTypes.SEPARATOR; }
  {SPACE}              { return AsciiDocTokenTypes.WHITE_SPACE; }
  "=\"" ( [^\"] | "\\\"" )* "\"" { return AsciiDocTokenTypes.BLOCK_ATTR_VALUE; }
  [^\],=\n\t ]+ {
        if (!yytext().toString().startsWith(".") && !yytext().toString().startsWith("%") && !yytext().toString().equals("role")) {
          setStyle(yytext().toString());
        }
        return AsciiDocTokenTypes.BLOCK_ATTR_NAME;
      }
  [^]                  { return AsciiDocTokenTypes.BLOCK_ATTR_NAME; }
}

<BLOCK_MACRO> {
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  "["                  { yybegin(BLOCK_MACRO_ATTRS); return AsciiDocTokenTypes.BLOCK_ATTRS_START; }
  [^]                  { return AsciiDocTokenTypes.BLOCK_MACRO_BODY; }
}

<BLOCK_MACRO_ATTRS> {
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  "]"                  { yypopstate(); return AsciiDocTokenTypes.BLOCK_ATTRS_END; }
  ","                  { return AsciiDocTokenTypes.SEPARATOR; }
  {SPACE}              { return AsciiDocTokenTypes.WHITE_SPACE; }
  "=\"" ( [^\"] | "\\\"" )* "\"" { return AsciiDocTokenTypes.BLOCK_ATTR_VALUE; }
  [^]                  { return AsciiDocTokenTypes.BLOCK_ATTR_NAME; }
}

<INLINE_MACRO> {
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  "["                  { yybegin(INLINE_MACRO_ATTRS); return AsciiDocTokenTypes.INLINE_ATTRS_START; }
  [^]                  { return AsciiDocTokenTypes.INLINE_MACRO_BODY; }
}

<INLINE_MACRO_ATTRS> {
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  "]"                  { yypopstate(); return AsciiDocTokenTypes.INLINE_ATTRS_END; }
  ","                  { return AsciiDocTokenTypes.SEPARATOR; }
  {SPACE}              { return AsciiDocTokenTypes.WHITE_SPACE; }
  "=\"" ( [^\"] | "\\\"" )* "\"" { return AsciiDocTokenTypes.INLINE_ATTR_VALUE; }
  [^]                  { return AsciiDocTokenTypes.INLINE_ATTR_NAME; }
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
  ^ {LISTING_BLOCK_DELIMITER} $ {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yybegin(PREBLOCK);
      return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER;
    } else {
      return AsciiDocTokenTypes.LISTING_TEXT;
    }
  }
  // duplicating to handle end of file content
  ^ {LISTING_BLOCK_DELIMITER} / [^\-\n \t] {
    return AsciiDocTokenTypes.LISTING_TEXT;
  }
  ^ {LISTING_BLOCK_DELIMITER} | {MARKDOWN_LISTING_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yybegin(PREBLOCK);
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
      yyinitialIfNotInBlock();
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

<PASSTRHOUGH_INLINE> {
  {PASSTRHOUGH_INLINE} { yybegin(PREBLOCK); return AsciiDocTokenTypes.PASSTRHOUGH_INLINE_END; }
  [^]                  { return AsciiDocTokenTypes.PASSTRHOUGH_CONTENT; }
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
<LITERAL_BLOCK, LISTING_BLOCK, PASSTRHOUGH_BLOCK, LISTING_NO_DELIMITER, SINGLELINE> {
  ^ "include::" / [^\[\n]* "[" [^\]\n]* "]" {SPACE}* \n { yypushstate(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  ^ "include::" / [^\[\n]* {AUTOCOMPLETE} { yypushstate(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
}


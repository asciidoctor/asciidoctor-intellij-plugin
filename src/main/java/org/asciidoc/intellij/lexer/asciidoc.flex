package org.asciidoc.intellij.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class _AsciiDocLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

%{
 int blockDelimiterLength;
%}

SPACE = [\ \t]
NON_SPACE = [^\n]
LINE_COMMENT="//"[^\n]*
COMMENT_BLOCK_DELIMITER = "////" "/"* {SPACE}* \n
PASSTRHOUGH_BLOCK_DELIMITER = "++++" "+"* {SPACE}* \n
LISTING_BLOCK_DELIMITER = "----" "-"* {SPACE}* \n
EXAMPLE_BLOCK_DELIMITER = "====" "="* {SPACE}* \n
SIDEBAR_BLOCK_DELIMITER = "****" "*"* {SPACE}* \n
QUOTE_BLOCK_DELIMITER = "____" "_"* {SPACE}* \n
HEADING_START = "="{1,6} {SPACE}+
HEADING_START_MARKDOWN = "#"{1,6} {SPACE}+
// starting at the start of the line, but not with a dot
// next line follwoing with only header marks
HEADING_OLDSTYLE = [^.\n\t\[].* "\n" [-=~\^+]+ {SPACE}* "\n"
BLOCK_MACRO_START = [a-zA-Z0-9_]+"::"
TITLE_START = "."
BLOCK_ATTRS_START = "["

%state INSIDE_LINE
%state HEADING

%state LISTING_BLOCK
%state INSIDE_LISTING_BLOCK_LINE

%state COMMENT_BLOCK
%state INSIDE_COMMENT_BLOCK_LINE

%state EXAMPLE_BLOCK
%state INSIDE_EXAMPLE_BLOCK_LINE

%state PASSTRHOUGH_BLOCK
%state INSIDE_PASSTRHOUGH_BLOCK_LINE

%state SIDEBAR_BLOCK
%state INSIDE_SIDEBAR_BLOCK_LINE

%state QUOTE_BLOCK
%state INSIDE_QUOTE_BLOCK_LINE

%state BLOCK_MACRO
%state BLOCK_MACRO_ATTRS
%state TITLE
%state BLOCK_ATTRS

%%

<YYINITIAL> {
  {LISTING_BLOCK_DELIMITER}  { yybegin(LISTING_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER; }
  {COMMENT_BLOCK_DELIMITER} { yybegin(COMMENT_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.BLOCK_COMMENT; }
  {EXAMPLE_BLOCK_DELIMITER} { yybegin(EXAMPLE_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.EXAMPLE_BLOCK_DELIMITER; }
  {PASSTRHOUGH_BLOCK_DELIMITER} { yybegin(PASSTRHOUGH_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER; }
  {SIDEBAR_BLOCK_DELIMITER} { yybegin(SIDEBAR_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.SIDEBAR_BLOCK_DELIMITER; }
  {QUOTE_BLOCK_DELIMITER} { yybegin(QUOTE_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.QUOTE_BLOCK_DELIMITER; }

  {HEADING_OLDSTYLE} {
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
         && !heading.matches("^\\+*$")) {
        // push back the second newline of the pattern
        yypushback(1);
        return AsciiDocTokenTypes.HEADING;
      } else {
        yypushback(yylength()-1); // heading.length() + 1
        yybegin(INSIDE_LINE);
        return AsciiDocTokenTypes.TEXT;
      }
    }

  {LINE_COMMENT}       { return AsciiDocTokenTypes.LINE_COMMENT; }
  {HEADING_START} / {NON_SPACE} { yybegin(HEADING); return AsciiDocTokenTypes.HEADING; }
  {HEADING_START_MARKDOWN} / {NON_SPACE} { yybegin(HEADING); return AsciiDocTokenTypes.HEADING; }
  {TITLE_START} / [^\. ] { yybegin(TITLE); return AsciiDocTokenTypes.TITLE; }
  {BLOCK_MACRO_START} / {NON_SPACE} { yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  {BLOCK_ATTRS_START} { yybegin(BLOCK_ATTRS); return AsciiDocTokenTypes.BLOCK_ATTRS_START; }

  "\n"                 { return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.TEXT; }
}

<INSIDE_LINE> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.TEXT; }
}

<HEADING> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.HEADING; }
}

<TITLE> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.TITLE; }
}

<BLOCK_ATTRS> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  "]"                  { yybegin(YYINITIAL); return AsciiDocTokenTypes.BLOCK_ATTRS_END; }
  [^]                  { return AsciiDocTokenTypes.BLOCK_ATTR_NAME; }
}

<BLOCK_MACRO> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  "["                  { yybegin(BLOCK_MACRO_ATTRS); return AsciiDocTokenTypes.BLOCK_MACRO_ATTRIBUTES; }
  [^]                  { return AsciiDocTokenTypes.BLOCK_MACRO_BODY; }
}

<BLOCK_MACRO_ATTRS> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.BLOCK_MACRO_ATTRIBUTES; }
}

<LISTING_BLOCK> {
  {LISTING_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER;
    } else {
      return AsciiDocTokenTypes.LISTING_TEXT;
    }
  }
}

<LISTING_BLOCK, INSIDE_LISTING_BLOCK_LINE> {
  "\n"                 { yybegin(LISTING_BLOCK); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { yybegin(INSIDE_LISTING_BLOCK_LINE); return AsciiDocTokenTypes.LISTING_TEXT; }
}

<COMMENT_BLOCK> {
  {COMMENT_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.BLOCK_COMMENT;
    } else {
      return AsciiDocTokenTypes.BLOCK_COMMENT;
    }
  }
}

<COMMENT_BLOCK, INSIDE_COMMENT_BLOCK_LINE> {
  "\n"                 { yybegin(COMMENT_BLOCK); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { yybegin(INSIDE_COMMENT_BLOCK_LINE); return AsciiDocTokenTypes.BLOCK_COMMENT; }
}

<EXAMPLE_BLOCK> {
  {EXAMPLE_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.EXAMPLE_BLOCK_DELIMITER;
    } else {
      return AsciiDocTokenTypes.EXAMPLE_BLOCK;
    }
  }
}

<EXAMPLE_BLOCK, INSIDE_EXAMPLE_BLOCK_LINE> {
  "\n"                 { yybegin(EXAMPLE_BLOCK); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { yybegin(INSIDE_EXAMPLE_BLOCK_LINE); return AsciiDocTokenTypes.EXAMPLE_BLOCK; }
}

<PASSTRHOUGH_BLOCK> {
  {PASSTRHOUGH_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER;
    } else {
      return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK;
    }
  }
}

<PASSTRHOUGH_BLOCK, INSIDE_PASSTRHOUGH_BLOCK_LINE> {
  "\n"                 { yybegin(PASSTRHOUGH_BLOCK); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { yybegin(INSIDE_PASSTRHOUGH_BLOCK_LINE); return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK; }
}

<SIDEBAR_BLOCK> {
  {SIDEBAR_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.SIDEBAR_BLOCK_DELIMITER;
    } else {
      return AsciiDocTokenTypes.SIDEBAR_BLOCK;
    }
  }
}

<SIDEBAR_BLOCK, INSIDE_SIDEBAR_BLOCK_LINE> {
  "\n"                 { yybegin(SIDEBAR_BLOCK); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { yybegin(INSIDE_SIDEBAR_BLOCK_LINE); return AsciiDocTokenTypes.SIDEBAR_BLOCK; }
}

<QUOTE_BLOCK> {
  {QUOTE_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.QUOTE_BLOCK_DELIMITER;
    } else {
      return AsciiDocTokenTypes.QUOTE_BLOCK;
    }
  }
}

<QUOTE_BLOCK, INSIDE_QUOTE_BLOCK_LINE> {
  "\n"                 { yybegin(QUOTE_BLOCK); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { yybegin(INSIDE_QUOTE_BLOCK_LINE); return AsciiDocTokenTypes.QUOTE_BLOCK; }
}

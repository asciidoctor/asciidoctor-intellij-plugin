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

SPACE = [\ \t]
NON_SPACE = [^\n]
LINE_COMMENT="//"[^\n]*
LISTING_DELIMITER = "----" "-"*
COMMENT_BLOCK_DELIMITER = "////" "/"*
EXAMPLE_BLOCK_DELIMITER = "====" "="* "\n"
SIDEBAR_BLOCK_DELIMITER = "****" "*"* "\n"
HEADING_START = "="{1,6} {SPACE}+
BLOCK_MACRO_START = [a-zA-Z0-9_]+"::"
TITLE_START = "."
BLOCK_ATTRS_START = "["

%state INSIDE_LINE
%state LISTING
%state INSIDE_LISTING_LINE
%state HEADING
%state COMMENT_BLOCK
%state INSIDE_COMMENT_BLOCK_LINE
%state BLOCK_MACRO
%state BLOCK_MACRO_ATTRS
%state TITLE
%state BLOCK_ATTRS

%%

<YYINITIAL> {
  {COMMENT_BLOCK_DELIMITER} { yybegin(COMMENT_BLOCK); return AsciiDocTokenTypes.BLOCK_COMMENT; }
  {LINE_COMMENT}       { return AsciiDocTokenTypes.LINE_COMMENT; }
  {LISTING_DELIMITER}  { yybegin(LISTING); return AsciiDocTokenTypes.LISTING_DELIMITER; }
  {EXAMPLE_BLOCK_DELIMITER} { return AsciiDocTokenTypes.EXAMPLE_BLOCK_DELIMITER; }
  {SIDEBAR_BLOCK_DELIMITER} { return AsciiDocTokenTypes.SIDEBAR_BLOCK_DELIMITER; }
  {HEADING_START} / {NON_SPACE} { yybegin(HEADING); return AsciiDocTokenTypes.HEADING; }
  {TITLE_START} { yybegin(TITLE); return AsciiDocTokenTypes.TITLE; }
  {BLOCK_MACRO_START} / {NON_SPACE} { yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  {BLOCK_ATTRS_START} { yybegin(BLOCK_ATTRS); return AsciiDocTokenTypes.BLOCK_ATTRS_START; }

  "\n"                 { return AsciiDocTokenTypes.LINE_BREAK; }
  .                    { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.TEXT; }
}

<INSIDE_LINE> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  .                    { return AsciiDocTokenTypes.TEXT; }
}

<HEADING> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  .                    { return AsciiDocTokenTypes.HEADING; }
}

<TITLE> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  .                    { return AsciiDocTokenTypes.TITLE; }
}

<BLOCK_ATTRS> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  "]"                  { yybegin(YYINITIAL); return AsciiDocTokenTypes.BLOCK_ATTRS_END; }
  .                    { return AsciiDocTokenTypes.BLOCK_ATTR_NAME; }
}

<BLOCK_MACRO> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  "["                  { yybegin(BLOCK_MACRO_ATTRS); return AsciiDocTokenTypes.BLOCK_MACRO_ATTRIBUTES; }
  .                    { return AsciiDocTokenTypes.BLOCK_MACRO_BODY; }
}

<BLOCK_MACRO_ATTRS> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  .                    { return AsciiDocTokenTypes.BLOCK_MACRO_ATTRIBUTES; }
}

<LISTING, INSIDE_LISTING_LINE> {
  "\n"                 { yybegin(LISTING); return AsciiDocTokenTypes.LINE_BREAK; }
  .                    { yybegin(INSIDE_LISTING_LINE); return AsciiDocTokenTypes.LISTING_TEXT; }
}

<LISTING> {
  {LISTING_DELIMITER}  { yybegin(YYINITIAL); return AsciiDocTokenTypes.LISTING_DELIMITER; }
}

<COMMENT_BLOCK> {
  {COMMENT_BLOCK_DELIMITER} { yybegin(YYINITIAL); return AsciiDocTokenTypes.BLOCK_COMMENT; }
}

<COMMENT_BLOCK, INSIDE_COMMENT_BLOCK_LINE> {
  "\n"                 { yybegin(COMMENT_BLOCK); return AsciiDocTokenTypes.BLOCK_COMMENT; }
  .                    { yybegin(INSIDE_COMMENT_BLOCK_LINE); return AsciiDocTokenTypes.BLOCK_COMMENT; }
}

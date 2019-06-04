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

  private Stack<Integer> stateStack = new Stack<Integer>();

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

  private void yypushstate (int state) {
    stateStack.push(state);
  }

  private void yypopstate () {
    if(stateStack.size() > 0) {
      yybegin(stateStack.pop());
    } else {
      yybegin(YYINITIAL);
    }
  }

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
AUTOCOMPLETE = "IntellijIdeaRulezzz " // CompletionUtilCore.DUMMY_IDENTIFIER
BLOCK_ATTRS_START = "["
STRING = {NON_SPACE}+ \n? // something that doesn't have an empty line
// something with a non-blank at the end, might contain a line break, but only if it doesn't separate the block
WORD = {SPACE}* [^\n]* {SPACE}* \n {SPACE}* [^\ \t\n] | {SPACE}* [^\n]*[^\ \t\n]
BOLD = "*"
BULLET = {SPACE}* "*"+ {SPACE}+
DOUBLEBOLD = {BOLD} {BOLD}
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
BLOCKIDSTART = "[["
BLOCKIDEND = "]]"
SINGLE_QUOTE = "'"
DOUBLE_QUOTE = "\""
TYPOGRAPHIC_QUOTE_START = "\"`"
TYPOGRAPHIC_QUOTE_END = "`\""
ANCHORSTART = "[#"
ANCHOREND = "]"
LINKSTART = "link:"
LINKTEXT_START = "["
LINKEND = "]"
ATTRIBUTE_NAME_START = ":"
ATTRIBUTE_NAME = [a-zA-Z0-9_]+ [a-zA-Z0-9_-]*
ATTRIBUTE_NAME_END = ":"
ATTRIBUTE_REF_START = "{"
ATTRIBUTE_REF_END = "}"

%state MULTILINE
%state INSIDE_LINE
%state REF
%state REFTEXT
%state REFAUTO
%state BLOCKID
%state BLOCKREFTEXT
%state HEADING
%state SINGLELINE
%state ANCHORID
%state ANCHORREFTEXT

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

%state ATTRIBUTE_DECL
%state ATTRIBUTE_NAME
%state ATTRIBUTE_VAL
%state ATTRIBUTE_REF_START
%state ATTRIBUTE_REF

%state LINKSTART
%state LINKFILE
%state LINKANCHOR
%state LINKTEXT
%state LINKEND

%%

// IntelliJ might do partial parsing from any YYINITIAL inside a document
// therefore only return here is no other state (i.e. bold) needs to be preserved
<YYINITIAL> {
  [^]                  { yypushback(yylength()); yybegin(MULTILINE); }
}

<MULTILINE> {
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
           && !heading.matches("^\\+*$")
           // only minus signs are never a heading but block (double minus), a horizontal rule (triple minus) or something else
           && !heading.matches("^-*$")) {
          // push back the second newline of the pattern
          yypushback(1);
          resetFormatting();
          return AsciiDocTokenTypes.HEADING;
        } else {
          // pass this contents to the single line rules (second priority)
          yypushback(yylength());
          yybegin(SINGLELINE);
        }
      }
  {ATTRIBUTE_NAME_START} / {ATTRIBUTE_NAME} {ATTRIBUTE_NAME_END} {
        yybegin(ATTRIBUTE_DECL);
        return AsciiDocTokenTypes.ATTRIBUTE_NAME_START;
      }
  [^]                  { yypushback(yylength()); yybegin(SINGLELINE); }
}

<ATTRIBUTE_DECL> {
  "\n"               { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                { yybegin(ATTRIBUTE_NAME); return AsciiDocTokenTypes.ATTRIBUTE_NAME; }
}

<ATTRIBUTE_NAME> {
  {ATTRIBUTE_NAME_END} { yybegin(ATTRIBUTE_VAL); return AsciiDocTokenTypes.ATTRIBUTE_NAME_END; }
  {ATTRIBUTE_NAME}   { return AsciiDocTokenTypes.ATTRIBUTE_NAME; }
  "\n"               { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                { yybegin(YYINITIAL); }
}

<ATTRIBUTE_VAL> {
  /*Value continue on the next line if the line is ended by a space followed by a backslash*/
  {SPACE} "\\" {SPACE}* "\n" { return AsciiDocTokenTypes.ATTRIBUTE_VAL; }
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.ATTRIBUTE_VAL; }
}

<SINGLELINE> {
  {LISTING_BLOCK_DELIMITER}  { resetFormatting(); yypushback(1); yybegin(LISTING_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER; }
  {COMMENT_BLOCK_DELIMITER} { resetFormatting(); yypushback(1); yybegin(COMMENT_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.BLOCK_COMMENT; }
  {EXAMPLE_BLOCK_DELIMITER} { resetFormatting(); yypushback(1); yybegin(EXAMPLE_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.EXAMPLE_BLOCK_DELIMITER; }
  {PASSTRHOUGH_BLOCK_DELIMITER} { resetFormatting(); yypushback(1); yybegin(PASSTRHOUGH_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER; }
  {SIDEBAR_BLOCK_DELIMITER} { resetFormatting(); yypushback(1); yybegin(SIDEBAR_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.SIDEBAR_BLOCK_DELIMITER; }
  {QUOTE_BLOCK_DELIMITER} { resetFormatting(); yypushback(1); yybegin(QUOTE_BLOCK); blockDelimiterLength = yytext().toString().trim().length(); return AsciiDocTokenTypes.QUOTE_BLOCK_DELIMITER; }

  {ANCHORSTART} / [^\]\n]+ {ANCHOREND} { resetFormatting(); yybegin(ANCHORID); return AsciiDocTokenTypes.BLOCKIDSTART; }
  {LINE_COMMENT}       { return AsciiDocTokenTypes.LINE_COMMENT; }
  {HEADING_START} / {NON_SPACE} { resetFormatting(); yybegin(HEADING); return AsciiDocTokenTypes.HEADING; }
  {HEADING_START_MARKDOWN} / {NON_SPACE} { resetFormatting(); yybegin(HEADING); return AsciiDocTokenTypes.HEADING; }
  {TITLE_START} / [^\. ] { resetFormatting(); yybegin(TITLE); return AsciiDocTokenTypes.TITLE; }
  {BLOCK_MACRO_START} / {NON_SPACE} { resetFormatting(); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
  {BLOCK_ATTRS_START} / [^\[] { yybegin(BLOCK_ATTRS); return AsciiDocTokenTypes.BLOCK_ATTRS_START; }

  {BULLET} / {STRING} { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.BULLET; }

  /* a blank line, it separates blocks. Don't return YYINITIAL here, as writing on a blank line might change the meaning
  of the previous blocks combined (for example there is now an italic formatting spanning the two combined blocks) */
  "\w"* "\n"           { resetFormatting(); yybegin(MULTILINE); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { yypushback(yylength()); yybegin(INSIDE_LINE); }
}

<INSIDE_LINE> {
  "\n"                 { yybegin(MULTILINE); return AsciiDocTokenTypes.LINE_BREAK; }
  [ \t]                { return AsciiDocTokenTypes.WHITE_SPACE; }
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
  {MONO} {MONO}? / [^\`\n \t] {WORD}* {MONO} { if(isUnconstrainedStart() && !singlemono && !doublemono) {
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
  {MONO}               { if(singlemono && !doublemono && isUnconstrainedEnd()) {
                           singlemono = false; return AsciiDocTokenTypes.MONO_END;
                         } else {
                           return textFormat();
                         }
                       }
  // ITALIC END
  {LPAREN}             { return AsciiDocTokenTypes.LPAREN; }
  {RPAREN}             { return AsciiDocTokenTypes.RPAREN; }
  {LBRACKET}           { return AsciiDocTokenTypes.LBRACKET; }
  {RBRACKET}           { return AsciiDocTokenTypes.RBRACKET; }
  {REFSTART} / [^>\n]+ {REFEND} { yybegin(REF); return AsciiDocTokenTypes.REFSTART; }
  // when typing a reference, it will not be complete due to the missing matching closing ref
  // therefore second variante for incomplete REF that will only be active during autocomplete
  {REFSTART} / [^>\n ]* {AUTOCOMPLETE} { yybegin(REFAUTO); return AsciiDocTokenTypes.REFSTART; }
  {BLOCKIDSTART} / [^\]\n]+ {BLOCKIDEND} { yybegin(BLOCKID); return AsciiDocTokenTypes.BLOCKIDSTART; }
  {ATTRIBUTE_REF_START} / {ATTRIBUTE_NAME} {ATTRIBUTE_REF_END} { yybegin(ATTRIBUTE_REF); return AsciiDocTokenTypes.ATTRIBUTE_REF_START; }
  {LT}                 { return AsciiDocTokenTypes.LT; }
  {GT}                 { return AsciiDocTokenTypes.GT; }
  {SINGLE_QUOTE}       { if (isUnconstrainedStart() || isUnconstrainedEnd()) {
                           return AsciiDocTokenTypes.SINGLE_QUOTE;
                         } else {
                           return textFormat();
                         }
                       }
  {DOUBLE_QUOTE}       { return AsciiDocTokenTypes.DOUBLE_QUOTE; }
  {TYPOGRAPHIC_QUOTE_START} / [^\*\n \t] {WORD}* {TYPOGRAPHIC_QUOTE_END} {
                         if (isUnconstrainedStart()) {
                           typographicquote = true;
                           return AsciiDocTokenTypes.TYPOGRAPHIC_QUOTE_START;
                         } else {
                           yypushback(1);
                           return AsciiDocTokenTypes.DOUBLE_QUOTE;
                         }
                       }
  {TYPOGRAPHIC_QUOTE_END} {
                         if (typographicquote && isUnconstrainedEnd()) {
                           typographicquote = false;
                           return AsciiDocTokenTypes.TYPOGRAPHIC_QUOTE_END;
                         } else {
                           yypushback(1);
                           return textFormat();
                         }
                       }
  {LINKSTART} / [^\[\n]* {LINKTEXT_START} [^\]\n]* {LINKEND} { yybegin(LINKFILE); return AsciiDocTokenTypes.LINKSTART; }
  [^]                  { return textFormat(); }
}

<REF, REFTEXT> {
  {REFEND}             { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.REFEND; }
}

<REF> {
  ","                  { yybegin(REFTEXT); return AsciiDocTokenTypes.SEPARATOR; }
  "#"                  { return AsciiDocTokenTypes.SEPARATOR; }
  [^#\]\n]+ / "#"      { return AsciiDocTokenTypes.REFFILE; }
  [^]                  { return AsciiDocTokenTypes.REF; }
}

<REFTEXT> {
  [^]                  { return AsciiDocTokenTypes.REFTEXT; }
}

<REFAUTO> {
  [ ,]                 { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.REF; }
  [^]                  { return AsciiDocTokenTypes.REF; }
}

<LINKFILE, LINKANCHOR> {
  {LINKTEXT_START}     { yybegin(LINKTEXT); return AsciiDocTokenTypes.LINKTEXT_START; }
}

<LINKFILE> {
  "#"                  { yybegin(LINKANCHOR); return AsciiDocTokenTypes.SEPARATOR; }
  [^]                  { return AsciiDocTokenTypes.LINKFILE; }
}

<LINKANCHOR> {
  [^]                  { return AsciiDocTokenTypes.LINKANCHOR; }
}

<LINKTEXT> {
  {LINKEND}            { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.LINKEND; }
  [^]                  { return AsciiDocTokenTypes.LINKTEXT; }
}

<ATTRIBUTE_REF_START, ATTRIBUTE_REF> {
  {ATTRIBUTE_REF_END}  { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.ATTRIBUTE_REF_END; }
}

<ATTRIBUTE_REF> {
  [^]                  { return AsciiDocTokenTypes.ATTRIBUTE_REF; }
}

<BLOCKID, BLOCKREFTEXT> {
  {BLOCKIDEND}         { yybegin(INSIDE_LINE); return AsciiDocTokenTypes.BLOCKIDEND; }
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
  [^]                  { return AsciiDocTokenTypes.HEADING; }
}

<TITLE> {
  "\n"                 { yybegin(YYINITIAL); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { return AsciiDocTokenTypes.TITLE; }
}

<BLOCK_ATTRS> {
  "\n"                 { yypopstate(); return AsciiDocTokenTypes.LINE_BREAK; }
  "]"                  { yypopstate(); return AsciiDocTokenTypes.BLOCK_ATTRS_END; }
  ","                  { return AsciiDocTokenTypes.SEPARATOR; }
  {SPACE}              { return AsciiDocTokenTypes.WHITE_SPACE; }
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
  [^]                  { return AsciiDocTokenTypes.BLOCK_MACRO_ATTRIBUTES; }
}

<LISTING_BLOCK> {
  {LISTING_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yypushback(1);
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER;
    } else {
      yypushback(1);
      return AsciiDocTokenTypes.LISTING_TEXT;
    }
  }
  // include is the only allowed block macro in this type of block
  "include::" / [^\[\n]* "[" [^\]\n]* "]" { yypushstate(LISTING_BLOCK); yybegin(BLOCK_MACRO); return AsciiDocTokenTypes.BLOCK_MACRO_ID; }
}

<LISTING_BLOCK, INSIDE_LISTING_BLOCK_LINE> {
  "\n"                 { yybegin(LISTING_BLOCK); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { yybegin(INSIDE_LISTING_BLOCK_LINE); return AsciiDocTokenTypes.LISTING_TEXT; }
}

<COMMENT_BLOCK> {
  {COMMENT_BLOCK_DELIMITER} {
    if (yytext().toString().trim().length() == blockDelimiterLength) {
      yypushback(1);
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.BLOCK_COMMENT;
    } else {
      yypushback(1);
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
      yypushback(1);
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.EXAMPLE_BLOCK_DELIMITER;
    } else {
      yypushback(1);
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
      yypushback(1);
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER;
    } else {
      yypushback(1);
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
      yypushback(1);
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.SIDEBAR_BLOCK_DELIMITER;
    } else {
      yypushback(1);
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
      yypushback(1);
      yybegin(YYINITIAL);
      return AsciiDocTokenTypes.QUOTE_BLOCK_DELIMITER;
    } else {
      yypushback(1);
      return AsciiDocTokenTypes.QUOTE_BLOCK;
    }
  }
}

<QUOTE_BLOCK, INSIDE_QUOTE_BLOCK_LINE> {
  "\n"                 { yybegin(QUOTE_BLOCK); return AsciiDocTokenTypes.LINE_BREAK; }
  [^]                  { yybegin(INSIDE_QUOTE_BLOCK_LINE); return AsciiDocTokenTypes.QUOTE_BLOCK; }
}

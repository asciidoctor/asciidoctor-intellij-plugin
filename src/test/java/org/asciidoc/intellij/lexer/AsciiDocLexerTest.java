package org.asciidoc.intellij.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.testFramework.LexerTestCase;

/**
 * @author yole
 */
public class AsciiDocLexerTest extends LexerTestCase {
  public void testSimple() {
    doTest("abc\ndef",
      "AsciiDoc:TEXT ('abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('def')");
  }

  public void testLineComment() {
    doTest("// foo\n// bar", "AsciiDoc:LINE_COMMENT ('// foo')\n" +
      "AsciiDoc:LINE_BREAK ('\\n')\n" +
      "AsciiDoc:LINE_COMMENT ('// bar')");
  }

  public void testListing() {
    doTest("some text at start\n----\nbbbb\n----\ncccc",
      "AsciiDoc:TEXT ('some')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('at')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('start')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('bbbb')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('cccc')");
  }

  public void testListingAtEndOfFile() {
    doTest("----\nlisting\n----",
      "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('listing')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')");
  }

  public void testListingWithInclude() {
    doTest("----\ninclude::file.adoc[]\n----\n",
      "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_MACRO_ID ('include::')\n" +
        "AsciiDoc:BLOCK_MACRO_BODY ('file.adoc')\n" +
        "AsciiDoc:BLOCK_ATTRS_START ('[')\n" +
        "AsciiDoc:BLOCK_ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testHeading() {
    doTest("= Abc\nabc\n== Def\ndef",
      "AsciiDoc:HEADING ('= Abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADING ('== Def')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('def')");
  }

  public void testTable() {
    doTest("|====\n" +
        "|1|2|3\n" +
        "|====",
      "AsciiDoc:BLOCK_DELIMITER ('|====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('|1|2|3')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('|====')");
  }

  public void testHeadingOldStyle() {
    doTest("Abc\n===\ndef",
      "AsciiDoc:HEADING ('Abc\\n===')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('def')");
  }

  public void testCommentBlock() {
    doTest("////\nfoo bar\n////\nabc",
      "AsciiDoc:BLOCK_COMMENT ('////')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_COMMENT ('foo bar')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_COMMENT ('////')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('abc')");
  }

  public void testBlockMacro() {
    doTest("image::foo.png[Caption]\nabc",
      "AsciiDoc:BLOCK_MACRO_ID ('image::')\n" +
        "AsciiDoc:BLOCK_MACRO_BODY ('foo.png')\n" +
        "AsciiDoc:BLOCK_ATTRS_START ('[')\n" +
        "AsciiDoc:BLOCK_MACRO_ATTRIBUTES ('Caption')\n" +
        "AsciiDoc:BLOCK_ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('abc')");
  }

  public void testExample() {
    doTest("====\nFoo\n====\n",
      "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Foo')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testTitle() {
    doTest(".Foo bar baz\nFoo bar baz",
      "AsciiDoc:TITLE ('.Foo bar baz')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Foo')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('bar')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('baz')");
  }

  public void testBlockAttrs() {
    doTest("[NOTE]\n",
      "AsciiDoc:BLOCK_ATTRS_START ('[')\n" +
        "AsciiDoc:BLOCK_ATTR_NAME ('NOTE')\n" +
        "AsciiDoc:BLOCK_ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testUnclosedBlockAttrs() {
    doTest("[\nfoo",
      "AsciiDoc:BLOCK_ATTRS_START ('[')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('foo')");
  }

  public void testOldStyleHeading() {
    doTest("Hi\n--\n",
      "AsciiDoc:HEADING ('Hi\\n--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }


  public void testAttributeUsage() {
    doTest("This is an {attribute} more text.",
      "AsciiDoc:TEXT ('This')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('is')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('an')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:ATTRIBUTE_REF_START ('{')\n" +
        "AsciiDoc:ATTRIBUTE_REF ('attribute')\n" +
        "AsciiDoc:ATTRIBUTE_REF_END ('}')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('more')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('text.')");
  }

  public void testAttributeWithoutValue() {
    doTest(":attribute:",
      "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')");
  }

  public void testAttributeEscaped() {
    doTest("\\:attribute:",
      "AsciiDoc:TEXT ('\\:attribute:')");
  }

  public void testAttributeWithValue() {
    doTest(":attribute: value",
      "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')\n" +
        "AsciiDoc:ATTRIBUTE_VAL (' value')");
  }

  /**
   * Value continue on the next line if the line is ended by a space followed by a backslash.
   */
  public void testAttributeMultiline() {
    doTest(":attribute: value \\\n continue on the next line\nMore text",
      "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')\n" +
        "AsciiDoc:ATTRIBUTE_VAL (' value \\\\n continue on the next line')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('More')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('text')");
  }

  public void testTwoConsecutiveAttributes() {
    doTest("Text\n:attribute1:\n:attribute2:",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute1')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute2')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')");
  }

  public void testContinuation() {
    doTest("+\n--\n",
      "AsciiDoc:CONTINUATION ('+')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testContinuationAfter() {
    doTest("--\n+\n",
      "AsciiDoc:BLOCK_DELIMITER ('--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:CONTINUATION ('+')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testBoldSimple() {
    doTest("Hello *bold* world",
      "AsciiDoc:TEXT ('Hello')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:BOLD ('bold')\n" +
        "AsciiDoc:BOLD_END ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('world')");
  }

  public void testBoldDouble() {
    doTest("Hello **bold** world",
      "AsciiDoc:TEXT ('Hello')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:BOLD_START ('**')\n" +
        "AsciiDoc:BOLD ('bold')\n" +
        "AsciiDoc:BOLD_END ('**')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('world')");
  }

  public void testNonBoldWithBlockBreak() {
    doTest("Hello **bold\n\n** world",
      "AsciiDoc:TEXT ('Hello')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('**bold')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BULLET ('**')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('world')");
  }

  public void testBoldAtBeginningAndEndOfLineSingle() {
    doTest("*bold*",
      "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:BOLD ('bold')\n" +
        "AsciiDoc:BOLD_END ('*')");
  }

  public void testSingleQuote() {
    doTest("'single'",
      "AsciiDoc:SINGLE_QUOTE (''')\n" +
        "AsciiDoc:TEXT ('single')\n" +
        "AsciiDoc:SINGLE_QUOTE (''')");
  }

  public void testNoSingleQuoteJustText() {
    doTest("don't",
      "AsciiDoc:TEXT ('don't')");
  }

  public void testItalicBlankAtEndOfFirstLine() {
    doTest("_test \ntest_",
      "AsciiDoc:ITALIC_START ('_')\n" +
        "AsciiDoc:ITALIC ('test')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:ITALIC ('test')\n" +
        "AsciiDoc:ITALIC_END ('_')");
  }

  public void testNonItalicAsPreceededByNewline() {
    doTest("_test\n_",
      "AsciiDoc:TEXT ('_test')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('_')");
  }

  public void testBoldMultipleInSingleLine() {
    doTest("bold *constrained* & **un**constrained",
      "AsciiDoc:TEXT ('bold')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:BOLD ('constrained')\n" +
        "AsciiDoc:BOLD_END ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('&')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:BOLD_START ('**')\n" +
        "AsciiDoc:BOLD ('un')\n" +
        "AsciiDoc:BOLD_END ('**')\n" +
        "AsciiDoc:TEXT ('constrained')");
  }

  public void testItalicMultipleInSingleLine() {
    doTest("italic _constrained_ & __un__constrained",
      "AsciiDoc:TEXT ('italic')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:ITALIC_START ('_')\n" +
        "AsciiDoc:ITALIC ('constrained')\n" +
        "AsciiDoc:ITALIC_END ('_')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('&')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:ITALIC_START ('__')\n" +
        "AsciiDoc:ITALIC ('un')\n" +
        "AsciiDoc:ITALIC_END ('__')\n" +
        "AsciiDoc:TEXT ('constrained')");
  }

  public void testMonoMultipleInSingleLine() {
    doTest("mono `constrained` & ``un``constrained",
      "AsciiDoc:TEXT ('mono')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:MONO_START ('`')\n" +
        "AsciiDoc:MONO ('constrained')\n" +
        "AsciiDoc:MONO_END ('`')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('&')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:MONO_START ('``')\n" +
        "AsciiDoc:MONO ('un')\n" +
        "AsciiDoc:MONO_END ('``')\n" +
        "AsciiDoc:TEXT ('constrained')");
  }

  public void testMonoItalicBold() {
    doTest("``**__un__**``constrained",
      "AsciiDoc:MONO_START ('``')\n" +
        "AsciiDoc:BOLD_START ('**')\n" +
        "AsciiDoc:ITALIC_START ('__')\n" +
        "AsciiDoc:MONOBOLDITALIC ('un')\n" +
        "AsciiDoc:ITALIC_END ('__')\n" +
        "AsciiDoc:BOLD_END ('**')\n" +
        "AsciiDoc:MONO_END ('``')\n" +
        "AsciiDoc:TEXT ('constrained')");
  }

  public void testBoldAtBeginningAndEndOfLineDouble() {
    doTest("**bold**",
      "AsciiDoc:BOLD_START ('**')\n" +
        "AsciiDoc:BOLD ('bold')\n" +
        "AsciiDoc:BOLD_END ('**')");
  }

  public void testNonMatchingBoldHead() {
    doTest("**bold*",
      "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:BOLD ('*bold')\n" +
        "AsciiDoc:BOLD_END ('*')");
  }

  public void testNonMatchingBoldTail() {
    doTest("*bold**",
      "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:BOLD ('bold*')\n" +
        "AsciiDoc:BOLD_END ('*')");
  }

  public void testUnconstrainedNonBold() {
    doTest("x*nonbold*x",
      "AsciiDoc:TEXT ('x*nonbold*x')");
  }

  public void testUnconstrainedNonItalic() {
    doTest("x_nonitalic_x",
      "AsciiDoc:TEXT ('x_nonitalic_x')");
  }

  public void testUnconstrainedNonMono() {
    doTest("x`nonmono`x",
      "AsciiDoc:TEXT ('x`nonmono`x')");
  }

  public void testSpecialUnderscore() {
    doTest("x__*italiconly*__x",
      "AsciiDoc:TEXT ('x')\n" +
        "AsciiDoc:ITALIC_START ('__')\n" +
        "AsciiDoc:ITALIC ('*italiconly*')\n" +
        "AsciiDoc:ITALIC_END ('__')\n" +
        "AsciiDoc:TEXT ('x')");
  }

  public void testBoldItalic() {
    doTest("*_bolditalic_*",
      "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:ITALIC_START ('_')\n" +
        "AsciiDoc:BOLDITALIC ('bolditalic')\n" +
        "AsciiDoc:ITALIC_END ('_')\n" +
        "AsciiDoc:BOLD_END ('*')");
  }

  public void testConstrainedMustNotEndWithBlankBold() {
    doTest("*test * test*",
      "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:BOLD ('test')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:BOLD ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:BOLD ('test')\n" +
        "AsciiDoc:BOLD_END ('*')");
  }

  public void testConstrainedMustNotEndWithBlankItalic() {
    doTest("_test _ test_",
      "AsciiDoc:ITALIC_START ('_')\n" +
        "AsciiDoc:ITALIC ('test')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:ITALIC ('_')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:ITALIC ('test')\n" +
        "AsciiDoc:ITALIC_END ('_')");
  }

  public void testConstrainedMustNotEndWithBlankMono() {
    doTest("`test ` test`",
      "AsciiDoc:MONO_START ('`')\n" +
        "AsciiDoc:MONO ('test')\n" +
        "AsciiDoc:WHITE_SPACE_MONO (' ')\n" +
        "AsciiDoc:MONO ('`')\n" +
        "AsciiDoc:WHITE_SPACE_MONO (' ')\n" +
        "AsciiDoc:MONO ('test')\n" +
        "AsciiDoc:MONO_END ('`')");
  }

  public void testBullet() {
    doTest("* bullet",
      "AsciiDoc:BULLET ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('bullet')");
  }

  public void testBulletWithBlanksInFront() {
    doTest("  * bullet",
      "AsciiDoc:WHITE_SPACE ('  ')\n" +
        "AsciiDoc:BULLET ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('bullet')");
  }

  public void testMultipleBullets() {
    doTest("* bullet1\n* bullet2",
      "AsciiDoc:BULLET ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('bullet1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BULLET ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('bullet2')");
  }

  public void testMultipleBulletsLevel2() {
    doTest("** bullet1\n** bullet2",
      "AsciiDoc:BULLET ('**')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('bullet1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BULLET ('**')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('bullet2')");
  }

  public void testThreeBulletItems() {
    doTest("* abc\n" +
        "* def\n" +
        "* ghi\n",
      "AsciiDoc:BULLET ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BULLET ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('def')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BULLET ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('ghi')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testSidebar() {
    doTest("****\nFoo\n****\n",
      "AsciiDoc:BLOCK_DELIMITER ('****')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Foo')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('****')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testRef() {
    doTest("Text <<REF>> More Text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:REFSTART ('<<')\n" +
        "AsciiDoc:REF ('REF')\n" +
        "AsciiDoc:REFEND ('>>')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('More')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testEscapedRef() {
    doTest("\\<<REF>>",
      "AsciiDoc:TEXT ('\\')\n" +
        "AsciiDoc:LT ('<')\n" +
        "AsciiDoc:LT ('<')\n" +
        "AsciiDoc:TEXT ('REF')\n" +
        "AsciiDoc:GT ('>')\n" +
        "AsciiDoc:GT ('>')");
  }

  public void testRefWithFile() {
    doTest("Text <<FILE#REF>> More Text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:REFSTART ('<<')\n" +
        "AsciiDoc:REFFILE ('FILE')\n" +
        "AsciiDoc:SEPARATOR ('#')\n" +
        "AsciiDoc:REF ('REF')\n" +
        "AsciiDoc:REFEND ('>>')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('More')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testRefWithRefText() {
    doTest("Text <<REF,Text>> More Text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:REFSTART ('<<')\n" +
        "AsciiDoc:REF ('REF')\n" +
        "AsciiDoc:SEPARATOR (',')\n" +
        "AsciiDoc:REFTEXT ('Text')\n" +
        "AsciiDoc:REFEND ('>>')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('More')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testEscapedLink() {
    doTest("\\link:FILE[Text]",
      "AsciiDoc:TEXT ('\\link:FILE')\n" +
        "AsciiDoc:LBRACKET ('[')\n" +
        "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:RBRACKET (']')");
  }

  public void testLinkWithAnchor() {
    doTest("Text link:FILE#ANCHOR[Text] More Text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:LINKFILE ('FILE')\n" +
        "AsciiDoc:SEPARATOR ('#')\n" +
        "AsciiDoc:LINKANCHOR ('ANCHOR')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKTEXT ('Text')\n" +
        "AsciiDoc:LINKEND (']')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('More')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testLinkForAutocomplete() {
    doTest("Text link:FILEIntellijIdeaRulezzz More Text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:LINKFILE ('FILEIntellijIdeaRulezzz')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('More')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testBlockid() {
    doTest("[[BLOCKID]] Text",
      "AsciiDoc:BLOCKIDSTART ('[[')\n" +
        "AsciiDoc:BLOCKID ('BLOCKID')\n" +
        "AsciiDoc:BLOCKIDEND (']]')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testBlockidWithRefText() {
    doTest("[[BLOCKID,name]] Text",
      "AsciiDoc:BLOCKIDSTART ('[[')\n" +
        "AsciiDoc:BLOCKID ('BLOCKID')\n" +
        "AsciiDoc:SEPARATOR (',')\n" +
        "AsciiDoc:BLOCKREFTEXT ('name')\n" +
        "AsciiDoc:BLOCKIDEND (']]')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testAnchorid() {
    doTest("[#BLOCKID]Text",
      "AsciiDoc:BLOCKIDSTART ('[#')\n" +
        "AsciiDoc:BLOCKID ('BLOCKID')\n" +
        "AsciiDoc:BLOCKIDEND (']')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testAnchorWithRefText() {
    doTest("[#BLOCKID,name]Text",
      "AsciiDoc:BLOCKIDSTART ('[#')\n" +
        "AsciiDoc:BLOCKID ('BLOCKID')\n" +
        "AsciiDoc:SEPARATOR (',')\n" +
        "AsciiDoc:BLOCKREFTEXT ('name')\n" +
        "AsciiDoc:BLOCKIDEND (']')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testAnchorWithClass() {
    doTest("[#BLOCKID.class]Text",
      "AsciiDoc:BLOCKIDSTART ('[#')\n" +
        "AsciiDoc:BLOCKID ('BLOCKID')\n" +
        "AsciiDoc:SEPARATOR ('.')\n" +
        "AsciiDoc:BLOCKREFTEXT ('class')\n" +
        "AsciiDoc:BLOCKIDEND (']')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testEscapedBold() {
    doTest("Text \\*nonbold* Text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('\\*nonbold*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testTypographicDoubleQuotes() {
    doTest("\"`typoquote`\"",
      "AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('\"`')\n" +
        "AsciiDoc:TEXT ('typoquote')\n" +
        "AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`\"')");
  }

  public void testTypographicSingleQuotes() {
    doTest("'`typoquote`'",
      "AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_START (''`')\n" +
        "AsciiDoc:TEXT ('typoquote')\n" +
        "AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_END ('`'')");
  }

  public void testMonospaceWithQuotes() {
    doTest("`\"initial value\"`",
      "AsciiDoc:MONO_START ('`')\n" +
        "AsciiDoc:DOUBLE_QUOTE ('\"')\n" +
        "AsciiDoc:MONO ('initial')\n" +
        "AsciiDoc:WHITE_SPACE_MONO (' ')\n" +
        "AsciiDoc:MONO ('value')\n" +
        "AsciiDoc:DOUBLE_QUOTE ('\"')\n" +
        "AsciiDoc:MONO_END ('`')");
  }

  public void testNoTypographicQuotes() {
    doTest("\"` test `\"",
      "AsciiDoc:DOUBLE_QUOTE ('\"')\n" +
        "AsciiDoc:TEXT ('`')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('test')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('`')\n" +
        "AsciiDoc:DOUBLE_QUOTE ('\"')");
  }

  public void testNoTypographicQuotesNonMatching() {
    doTest("\"`test",
      "AsciiDoc:DOUBLE_QUOTE ('\"')\n" +
        "AsciiDoc:TEXT ('`test')");
  }

  public void testPassThroughInline() {
    doTest("+++pt\npt2+++",
      "AsciiDoc:PASSTRHOUGH_INLINE_START ('+++')\n" +
        "AsciiDoc:PASSTRHOUGH_CONTENT ('pt\\npt2')\n" +
        "AsciiDoc:PASSTRHOUGH_INLINE_END ('+++')");
  }

  public void testLiteralBlock() {
    doTest("....\nliteral\n....\n",
      "AsciiDoc:LITERAL_BLOCK_DELIMITER ('....')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LITERAL_BLOCK ('literal')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LITERAL_BLOCK_DELIMITER ('....')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testQuotedBlock() {
    doTest("____\nQuoted with *bold*\n____\n",
      "AsciiDoc:BLOCK_DELIMITER ('____')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Quoted')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('with')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:BOLD ('bold')\n" +
        "AsciiDoc:BOLD_END ('*')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('____')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testNestedQuotedBlock() {
    doTest("____\nQuoted\n_____\nDoubleQuote\n_____\n____\n",
      "AsciiDoc:BLOCK_DELIMITER ('____')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Quoted')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('_____')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('DoubleQuote')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('_____')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('____')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testBlockEndingOverOldStyleHeader() {
    doTest("--\nS\n--\n",
      "AsciiDoc:BLOCK_DELIMITER ('--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('S')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testHorizontalRule() {
    doTest("---\n",
      "AsciiDoc:HORIZONTALRULE ('---')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testPagebreak() {
    doTest("<<<\n",
      "AsciiDoc:PAGEBREAK ('<<<')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testValueInBlockAttribute() {
    doTest("[cols=\"1,4\", options=\"header\"]",
      "AsciiDoc:BLOCK_ATTRS_START ('[')\n" +
        "AsciiDoc:BLOCK_ATTR_NAME ('cols')\n" +
        "AsciiDoc:BLOCK_ATTR_VALUE ('=\"1,4\"')\n" +
        "AsciiDoc:SEPARATOR (',')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:BLOCK_ATTR_NAME ('options')\n" +
        "AsciiDoc:BLOCK_ATTR_VALUE ('=\"header\"')\n" +
        "AsciiDoc:BLOCK_ATTRS_END (']')");
  }

  public void testEscapedBlockId() {
    doTest("\\[[id]]",
      "AsciiDoc:TEXT ('\\')\n" +
        "AsciiDoc:LBRACKET ('[')\n" +
        "AsciiDoc:LBRACKET ('[')\n" +
        "AsciiDoc:TEXT ('id')\n" +
        "AsciiDoc:RBRACKET (']')\n" +
        "AsciiDoc:RBRACKET (']')");
  }

  public void testEndOfSentence() {
    doTest("End. Of Sentence",
      "AsciiDoc:TEXT ('End')\n" +
        "AsciiDoc:END_OF_SENTENCE ('.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Of')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Sentence')");
  }

  public void testNoEndOfSentence() {
    doTest("End.No Sentence",
      "AsciiDoc:TEXT ('End.No')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Sentence')");
  }

  public void testDescription() {
    doTest("a property:: description",
      "AsciiDoc:DESCRIPTION ('a property::')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('description')");
  }

  @Override
  protected Lexer createLexer() {
    return new AsciiDocLexer();
  }

  @Override
  protected String getDirPath() {
    return null;
  }
}

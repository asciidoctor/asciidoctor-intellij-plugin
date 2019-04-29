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
        "AsciiDoc:TEXT ('some text at start')\n" +
            "AsciiDoc:LINE_BREAK ('\\n')\n" +
            "AsciiDoc:LISTING_BLOCK_DELIMITER ('----\\n')\n" +
            "AsciiDoc:LISTING_TEXT ('bbbb')\n" +
            "AsciiDoc:LINE_BREAK ('\\n')\n" +
            "AsciiDoc:LISTING_BLOCK_DELIMITER ('----\\n')\n" +
            "AsciiDoc:TEXT ('cccc')");
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

  public void testHeadingOldStyle() {
    doTest("Abc\n===\ndef",
        "AsciiDoc:HEADING ('Abc\\n===')\n" +
           "AsciiDoc:LINE_BREAK ('\\n')\n" +
           "AsciiDoc:TEXT ('def')");
  }

  public void testCommentBlock() {
    doTest("////\nfoo bar\n////\nabc",
        "AsciiDoc:BLOCK_COMMENT ('////\\nfoo bar')\n" +
            "AsciiDoc:LINE_BREAK ('\\n')\n" +
            "AsciiDoc:BLOCK_COMMENT ('////\\n')\n" +
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
    doTest("====\nFoo Bar Baz\n====\n",
        "AsciiDoc:EXAMPLE_BLOCK_DELIMITER ('====\\n')\n" +
            "AsciiDoc:EXAMPLE_BLOCK ('Foo Bar Baz')\n" +
            "AsciiDoc:LINE_BREAK ('\\n')\n" +
            "AsciiDoc:EXAMPLE_BLOCK_DELIMITER ('====\\n')");
  }

  public void testTitle() {
    doTest(".Foo bar baz\nFoo bar baz",
        "AsciiDoc:TITLE ('.Foo bar baz')\n" +
            "AsciiDoc:LINE_BREAK ('\\n')\n" +
            "AsciiDoc:TEXT ('Foo bar baz')");
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

  public void testContinuation() {
    doTest("+\n--\n",
      "AsciiDoc:TEXT ('+')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testContinuationAfter() {
    doTest("--\n+\n",
      "AsciiDoc:TEXT ('--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('+')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testBoldSimple() {
    doTest("Hello *bold* world",
      "AsciiDoc:TEXT ('Hello')\n" +
        "AsciiDoc:BOLD_START (' *')\n" +
        "AsciiDoc:BOLD ('bold')\n" +
        "AsciiDoc:BOLD_END ('*')\n" +
        "AsciiDoc:TEXT (' world')");
  }

  public void testBoldDouble() {
    doTest("Hello **bold** world",
      "AsciiDoc:TEXT ('Hello ')\n" +
        "AsciiDoc:BOLD_START ('**')\n" +
        "AsciiDoc:BOLD ('bold')\n" +
        "AsciiDoc:BOLD_END ('**')\n" +
        "AsciiDoc:TEXT (' world')");
  }

  public void testNonBoldWithBlockBreak() {
    doTest("Hello **bold\n\n** world",
      "AsciiDoc:TEXT ('Hello **bold')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BULLET ('** ')\n" +
        "AsciiDoc:TEXT ('world')");
  }

  public void testBoldAtBeginningAndEndOfLineSingle() {
    doTest("*bold*",
      "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:BOLD ('bold')\n" +
        "AsciiDoc:BOLD_END ('*')");
  }


  public void testBoldMultipleInSingleLine() {
    doTest("bold *constrained* & **un**constrained",
      "AsciiDoc:TEXT ('bold')\n" +
        "AsciiDoc:BOLD_START (' *')\n" +
        "AsciiDoc:BOLD ('constrained')\n" +
        "AsciiDoc:BOLD_END ('*')\n" +
        "AsciiDoc:TEXT (' & ')\n" +
        "AsciiDoc:BOLD_START ('**')\n" +
        "AsciiDoc:BOLD ('un')\n" +
        "AsciiDoc:BOLD_END ('**')\n" +
        "AsciiDoc:TEXT ('constrained')");
  }

  public void testItalicMultipleInSingleLine() {
    doTest("italic _constrained_ & __un__constrained",
      "AsciiDoc:TEXT ('italic ')\n" +
        "AsciiDoc:ITALIC_START ('_')\n" +
        "AsciiDoc:ITALIC ('constrained')\n" +
        "AsciiDoc:ITALIC_END ('_')\n" +
        "AsciiDoc:TEXT (' & ')\n" +
        "AsciiDoc:ITALIC_START ('__')\n" +
        "AsciiDoc:ITALIC ('un')\n" +
        "AsciiDoc:ITALIC_END ('__')\n" +
        "AsciiDoc:TEXT ('constrained')");
  }

  public void testMonoMultipleInSingleLine() {
    doTest("mono `constrained` & ``un``constrained",
      "AsciiDoc:TEXT ('mono ')\n" +
        "AsciiDoc:MONO_START ('`')\n" +
        "AsciiDoc:MONO ('constrained')\n" +
        "AsciiDoc:MONO_END ('`')\n" +
        "AsciiDoc:TEXT (' & ')\n" +
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
      "AsciiDoc:BOLD_START ('**')\n" +
        "AsciiDoc:BOLD ('bold')\n" +
        "AsciiDoc:BOLD_END ('*')");
  }

  public void testNonMatchingBoldTail() {
    doTest("*bold**",
      "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:BOLD ('bold*')\n" +
        "AsciiDoc:BOLD_END ('*')");
  }

  public void testBoldItalic() {
    doTest("*_bolditalic_*",
      "AsciiDoc:BOLD_START ('*')\n" +
        "AsciiDoc:ITALIC_START ('_')\n" +
        "AsciiDoc:BOLDITALIC ('bolditalic')\n" +
        "AsciiDoc:ITALIC_END ('_')\n" +
        "AsciiDoc:BOLD_END ('*')");
  }

  public void testBullet() {
    doTest("* bullet",
      "AsciiDoc:BULLET ('* ')\n" +
        "AsciiDoc:TEXT ('bullet')");
  }

  public void testMultipleBullets() {
    doTest("* bullet1\n* bullet2",
      "AsciiDoc:BULLET ('* ')\n" +
        "AsciiDoc:TEXT ('bullet1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BULLET ('* ')\n" +
        "AsciiDoc:TEXT ('bullet2')");
  }

  public void testMultipleBulletsLevel2() {
    doTest("** bullet1\n** bullet2",
      "AsciiDoc:BULLET ('** ')\n" +
        "AsciiDoc:TEXT ('bullet1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BULLET ('** ')\n" +
        "AsciiDoc:TEXT ('bullet2')");
  }

  public void testThreeBulletItems() {
    doTest("* abc\n" +
        "* def\n" +
        "* ghi\n",
      "AsciiDoc:BULLET ('* ')\n" +
        "AsciiDoc:TEXT ('abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BULLET ('* ')\n" +
        "AsciiDoc:TEXT ('def')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BULLET ('* ')\n" +
        "AsciiDoc:TEXT ('ghi')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testSidebar() {
    doTest("****\nFoo Bar Baz\n****\n",
        "AsciiDoc:SIDEBAR_BLOCK_DELIMITER ('****\\n')\n" +
            "AsciiDoc:SIDEBAR_BLOCK ('Foo Bar Baz')\n" +
            "AsciiDoc:LINE_BREAK ('\\n')\n" +
            "AsciiDoc:SIDEBAR_BLOCK_DELIMITER ('****\\n')");
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

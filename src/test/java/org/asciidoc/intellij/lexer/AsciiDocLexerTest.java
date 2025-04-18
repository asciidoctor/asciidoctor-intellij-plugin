package org.asciidoc.intellij.lexer;

import com.intellij.codeInsight.completion.CompletionUtilCore;
import com.intellij.lexer.Lexer;
import com.intellij.testFramework.LexerTestCase;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * Test cases covering lexing rules in asciidoc.flex.
 * To learn more about lexing, visit the contributor's guide for coders:
 * <a href="https://intellij-asciidoc-plugin.ahus1.de/docs/contributors-guide/coder/lexing-and-parsing.html">Lexing and parsing AsciiDoc files</a>.
 *
 * @author yole
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
@SuppressWarnings({"AsciiDocHeadingStyle", "AsciiDocLinkResolve", "AsciiDocAttributeContinuation", "AsciiDocReferenceResolve", "AsciiDocHorizontalRule", "AsciiDocXrefWithNaturalCrossReference", "AsciiDocAttributeShouldBeDefined"})
public class AsciiDocLexerTest extends LexerTestCase {
  public void testSimple() {
    doTest("abc\ndef",
      """
        AsciiDoc:TEXT ('abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('def')""");
  }

  public void testLineComment() {
    doTest("// foo\n// bar", """
      AsciiDoc:LINE_COMMENT ('// foo')
      AsciiDoc:LINE_BREAK ('\\n')
      AsciiDoc:LINE_COMMENT ('// bar')""");
  }

  public void testLineCommentAfterEnumeration() {
    doTest("* item\n// comment",
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('item')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LINE_COMMENT ('// comment')""");
  }

  public void testLineCommentWithCellCharacterInTable() {
    doTest("""
        |===
        //|comment
        |===
        """,
      """
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LINE_COMMENT ('//|comment')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testNoLineCommentWithCellCharacterInTable() {
    doTest("""
        |===
        |//comment
        |===
        """,
      """
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('//comment')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testLineCommentAndFormattingInCell() {
    doTest("""
        |===
        | _x_x_x
        // comment
        |===
        """,
      """
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('_x_x_x')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LINE_COMMENT ('// comment')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testPassthroughStartButThatIsText() {
    doTest("+ ",
      "AsciiDoc:TEXT ('+')\n" +
        "AsciiDoc:WHITE_SPACE (' ')");
  }

  public void testPassthroughEscaped() {
    doTest("\\+test *bold* test+",
      """
        AsciiDoc:TEXT ('\\+test')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:BOLD_START ('*')
        AsciiDoc:BOLD ('bold')
        AsciiDoc:BOLD_END ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('test+')""");
  }

  public void testListing() {
    doTest("some text at start\n----\nbbbb\n----\ncccc",
      """
        AsciiDoc:TEXT ('some')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('at')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('start')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('bbbb')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('cccc')""");
  }

  public void testListingWithSpaceDash() {
    doTest("----\nbbbb\n---- --\ncccc\n----\nText",
      """
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('bbbb')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('---- --')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('cccc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testTextLookingLikeListing() {
    doTest("---- --\nnolisting",
      """
        AsciiDoc:TEXT ('----')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('--')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('nolisting')""");
  }

  public void testListingAtEndOfFile() {
    doTest("----\nlisting\n----",
      """
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('listing')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')""");
  }

  public void testListingWithInclude() {
    doTest("----\ninclude::file.adoc[]\n----\n",
      """
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_MACRO_ID ('include::')
        AsciiDoc:BLOCK_MACRO_BODY ('file.adoc')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testIncludeWithTags() {
    doTest("include::file.adoc[tags=tag1;tag2]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('include::')
        AsciiDoc:BLOCK_MACRO_BODY ('file.adoc')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('tags')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:ATTR_VALUE ('tag1')
        AsciiDoc:ATTR_LIST_SEP (';')
        AsciiDoc:ATTR_VALUE ('tag2')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testIncludeWithIncompleteQuote() {
    doTest("include::file.adoc[tags='xx]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('include::')
        AsciiDoc:BLOCK_MACRO_BODY ('file.adoc')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('tags')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:SINGLE_QUOTE (''')
        AsciiDoc:ATTR_VALUE ('xx')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testIncludeWithBlankAndIncompleteQuote() {
    doTest("include::file.adoc[tags= 'xx]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('include::')
        AsciiDoc:BLOCK_MACRO_BODY ('file.adoc')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('tags')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:SINGLE_QUOTE (''')
        AsciiDoc:ATTR_VALUE ('xx')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testIncludeWithTagsQuotedWithBlank() {
    doTest("include::file.adoc[tags=\"tag1; tag2\"]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('include::')
        AsciiDoc:BLOCK_MACRO_BODY ('file.adoc')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('tags')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:ATTR_VALUE ('tag1')
        AsciiDoc:ATTR_LIST_SEP (';')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ATTR_VALUE ('tag2')
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testIncludeWithNegatedTag() {
    doTest("include::file.adoc[tags=\" !tag1!\"]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('include::')
        AsciiDoc:BLOCK_MACRO_BODY ('file.adoc')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('tags')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ATTR_LIST_OP ('!')
        AsciiDoc:ATTR_VALUE ('tag1!')
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testHeading() {
    doTest("= Abc\n\nabc\n== Def\ndef",
      """
        AsciiDoc:HEADING_TOKEN ('= Abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('==')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Def')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('def')""");
  }

  public void testDoctitleWithBlockId() {
    doTest("= [[id]] Title",
      """
        AsciiDoc:HEADING_TOKEN ('= ')
        AsciiDoc:INLINEIDSTART ('[[')
        AsciiDoc:BLOCKID ('id')
        AsciiDoc:INLINEIDEND (']]')
        AsciiDoc:HEADING_TOKEN (' Title')""");
  }

  public void testBibBlock() {
    doTest("= [[id]] Title",
      """
        AsciiDoc:HEADING_TOKEN ('= ')
        AsciiDoc:INLINEIDSTART ('[[')
        AsciiDoc:BLOCKID ('id')
        AsciiDoc:INLINEIDEND (']]')
        AsciiDoc:HEADING_TOKEN (' Title')""");
  }

  public void testSectionWithBlockId() {
    doTest("== [[id]] Section",
      """
        AsciiDoc:HEADING_TOKEN ('== ')
        AsciiDoc:INLINEIDSTART ('[[')
        AsciiDoc:BLOCKID ('id')
        AsciiDoc:INLINEIDEND (']]')
        AsciiDoc:HEADING_TOKEN (' Section')""");
  }

  public void testSectionWithBlockIdAndText() {
    doTest("== A [[id,text]] Section",
      """
        AsciiDoc:HEADING_TOKEN ('== A ')
        AsciiDoc:INLINEIDSTART ('[[')
        AsciiDoc:BLOCKID ('id')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:BLOCKREFTEXT ('text')
        AsciiDoc:INLINEIDEND (']]')
        AsciiDoc:HEADING_TOKEN (' Section')""");
  }

  public void testHeaderIfDef() {
    doTest("= Abc\nifdef::hi[]\nxx\nendif::[]",
      """
        AsciiDoc:HEADING_TOKEN ('= Abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_MACRO_ID ('ifdef::')
        AsciiDoc:ATTRIBUTE_REF ('hi')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('xx')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_MACRO_ID ('endif::')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testTable() {
    doTest("""
        |====
        |1|2|3

        |4|5|6
        |====""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('1')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('2')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('3')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('4')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('5')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('6')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|====')""");
  }

  public void testTableWithVerticalAlignment() {
    doTest("""
        |====
        .^|1
        |====""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('.^|')
        AsciiDoc:TEXT ('1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|====')""");
  }

  public void testTableWithColAndRowSpanAlignmentAndFormatting() {
    doTest("""
        |====
        2.2+^.^h|1
        |====""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('2.2+^.^h|')
        AsciiDoc:TEXT ('1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|====')""");
  }

  public void testTableWithColSpanningAlignmentAndFormatting() {
    doTest("""
        |====
        2+^.^h|1
        |====""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('2+^.^h|')
        AsciiDoc:TEXT ('1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|====')""");
  }

  public void testTableWithRowSpanningAlignmentAndFormatting() {
    doTest("""
        |====
        .2+^.^h|1
        |====""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('.2+^.^h|')
        AsciiDoc:TEXT ('1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|====')""");
  }

  public void testTableWithDuplicationAlignmentAndFormatting() {
    doTest("""
        |====
        2*^.^h|1
        |====""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('2*^.^h|')
        AsciiDoc:TEXT ('1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|====')""");
  }

  public void testCellWithTitle() {
    doTest("""
        |===
        a| .Title
        |===""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('a|')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TITLE_TOKEN ('.')
        AsciiDoc:TEXT ('Title')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|===')""");
  }

  public void testCellWithBlock() {
    doTest("""
        |===
        a|
        .Title
        | Cell
        |===""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('a|')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TITLE_TOKEN ('.')
        AsciiDoc:TEXT ('Title')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Cell')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|===')""");
  }

  public void testTableCellWithLeadingBlanks() {
    doTest("""
        |====
        |  1
        |====""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:WHITE_SPACE ('  ')
        AsciiDoc:TEXT ('1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|====')""");
  }

  public void testTableWithNoBlanks() {
    doTest("""
        |===
        |Name|Type|
        |===""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('Name')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('Type')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|===')""");
  }

  public void testHeadingOldStyle() {
    doTest("Abc\n===\n\ndef",
      """
        AsciiDoc:HEADING_OLDSTYLE ('Abc\\n===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('def')""");
  }

  public void testHeadingOldStyleWithHeaderSeparatedByBlankLine() {
    doTest("Abc\n===\nHeader\n\ndef",
      """
        AsciiDoc:HEADING_OLDSTYLE ('Abc\\n===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('def')""");
  }

  public void testHeadingOldStyleWithHeaderTwoLines() {
    doTest("Abc\n===\nHeader1\nHeader2\ndef",
      """
        AsciiDoc:HEADING_OLDSTYLE ('Abc\\n===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header2')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('def')""");
  }

  public void testNonHeadingAsItHasNoAlphanumericCharacter() {
    doTest("""
        ****
        ++++
        """,
      """
        AsciiDoc:BLOCK_DELIMITER ('****')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:PASSTRHOUGH_BLOCK_DELIMITER ('++++')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testNonHeadingAsItIsAComment() {
    doTest("""
        // c
        ++++
        """,
      """
        AsciiDoc:LINE_COMMENT ('// c')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:PASSTRHOUGH_BLOCK_DELIMITER ('++++')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testNoLineCommentWithThreeSlashes() {
    doTest("/// notacomment",
      """
        AsciiDoc:TEXT ('///')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('notacomment')""");
  }

  public void testVerySimpleComment() {
    doTest("//\n",
      "AsciiDoc:LINE_COMMENT ('//')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testHeadingNewStyleWithHeaderTwoLines() {
    doTest("= Abc\nHeader1\nHeader2\ndef",
      """
        AsciiDoc:HEADING_TOKEN ('= Abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header2')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('def')""");
  }

  public void testHeadingNewStyleWithHeaderTwoLinesAndLineComment() {
    doTest("= Abc\nHeader1 // no comment\n// Comment\nHeader2\ndef",
      """
        AsciiDoc:HEADING_TOKEN ('= Abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header1 // no comment')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LINE_COMMENT ('// Comment')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header2')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('def')""");
  }

  public void testHeadingNewStyleWithHeaderTwoLinesAndBlockComment() {
    doTest("= Abc\nHeader1\n////\nHiHo\n////\nHeader2\ndef",
      """
        AsciiDoc:HEADING_TOKEN ('= Abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_COMMENT ('////')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_COMMENT ('HiHo')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_COMMENT ('////')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header2')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('def')""");
  }

  public void testHeadingNewStyleWithInclude() {
    doTest("= Abc\ninclude::test.adoc[]\n",
      """
        AsciiDoc:HEADING_TOKEN ('= Abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_MACRO_ID ('include::')
        AsciiDoc:BLOCK_MACRO_BODY ('test.adoc')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testHeadingMarkdownStyleWithHeaderTwoLines() {
    //noinspection AsciiDocHeadingStyleInspection
    doTest("# Abc\nHeader1\nHeader2\ndef",
      """
        AsciiDoc:HEADING_TOKEN ('# Abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header2')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('def')""");
  }

  public void testHeadingNewStyleWithAppendixStyle() {
    doTest("[appendix]\n= Abc\nText\n",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('appendix')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADING_TOKEN ('= Abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Text')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testHeadingOldStyleWithHeaderTwoLinesAndAttribute() {
    doTest("Abc\n===\nHeader1\n:attr: val\nHeader2\ndef",
      """
        AsciiDoc:HEADING_OLDSTYLE ('Abc\\n===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attr')
        AsciiDoc:ATTRIBUTE_NAME_END (':')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ATTRIBUTE_VAL ('val')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADER ('Header2')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('def')""");
  }

  public void testCommentBlock() {
    doTest("////\nfoo bar\n////\nabc",
      """
        AsciiDoc:BLOCK_COMMENT ('////')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_COMMENT ('foo bar')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_COMMENT ('////')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('abc')""");
  }

  public void testNonCommentBlock() {
    doTest("////\\nfoo bar\\n////\\nabc",
      """
        AsciiDoc:TEXT ('////\\nfoo')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('bar\\n////\\nabc')""");
  }


  public void testNonCommentBlockCell() {
    doTest("|===\n|////\n|===",
      """
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('////')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|===')""");
  }

  public void testBlockMacro() {
    doTest("image::foo.png[Caption]\nabc",
      """
        AsciiDoc:BLOCK_MACRO_ID ('image::')
        AsciiDoc:BLOCK_MACRO_BODY ('foo.png')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('Caption')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('abc')""");
  }

  public void testBlockMacroWithBlockAttributesShouldClearStyle() {
    doTest("""
        [image]
        image::test.png[]

        Example Query
        [source]
        sql""",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('image')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_MACRO_ID ('image::')
        AsciiDoc:BLOCK_MACRO_BODY ('test.png')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('Example')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Query')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('source')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('sql')""");
  }

  public void testInlineMacroUrl() {
    doTest("image:http://image.com[Caption]\nabc",
      """
        AsciiDoc:INLINE_MACRO_ID ('image:')
        AsciiDoc:URL_LINK ('http://image.com')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('Caption')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('abc')""");
  }

  public void testBlockMacroWithAttribute() {
    doTest("macro::foo[key=value]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('macro::')
        AsciiDoc:BLOCK_MACRO_BODY ('foo')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('key')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:ATTR_VALUE ('value')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testBlockMacroWithSingleQuotedAttribute() {
    doTest("macro::foo[key='value']",
      """
        AsciiDoc:BLOCK_MACRO_ID ('macro::')
        AsciiDoc:BLOCK_MACRO_BODY ('foo')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('key')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:SINGLE_QUOTE (''')
        AsciiDoc:ATTR_VALUE ('value')
        AsciiDoc:SINGLE_QUOTE (''')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testBlockMacroWithDoubleQuotedAttribute() {
    doTest("macro::foo[key=\"value\"]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('macro::')
        AsciiDoc:BLOCK_MACRO_BODY ('foo')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('key')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:ATTR_VALUE ('value')
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testExample() {
    doTest("====\nFoo\n====\n",
      """
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Foo')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testTitle() {
    doTest(".Foo bar baz\nFoo bar baz",
      """
        AsciiDoc:TITLE_TOKEN ('.')
        AsciiDoc:TEXT ('Foo')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('bar')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('baz')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Foo')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('bar')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('baz')""");
  }

  public void testBlockAttrs() {
    doTest("[NOTE]\n",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('NOTE')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testAdmonition() {
    doTest("""
        [NOTE]
        ====
        TTT
        ====""",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('NOTE')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('TTT')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')""");
  }

  public void testNoBlockAttrs() {
    doTest("[nolink]:: Term",
      """
        AsciiDoc:LBRACKET ('[')
        AsciiDoc:DESCRIPTION ('nolink')
        AsciiDoc:RBRACKET (']')
        AsciiDoc:DESCRIPTION_END ('::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Term')""");
  }

  public void testUnclosedBlockAttrs() {
    doTest("[\nfoo",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('foo')""");
  }

  public void testOldStyleHeading() {
    doTest("Hi\n--\n",
      "AsciiDoc:HEADING_OLDSTYLE ('Hi\\n--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testBlockTypeLookingLikeAHeading() {
    doTest("[TIP]\n=====\nTip\n=====\n",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('TIP')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('=====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Tip')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('=====')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }


  public void testAttributeUsage() {
    doTest("This is an {attribute} more text.",
      """
        AsciiDoc:TEXT ('This')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('is')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('an')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ATTRIBUTE_REF_START ('{')
        AsciiDoc:ATTRIBUTE_REF ('attribute')
        AsciiDoc:ATTRIBUTE_REF_END ('}')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('more')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text.')""");
  }

  public void testEscapedAttribute() {
    doTest("\\{ ",
      "AsciiDoc:TEXT ('\\{')\n" +
        "AsciiDoc:WHITE_SPACE (' ')");
  }

  public void testEscapedInHeading() {
    doTest("== Heading \\{esc} word",
      "AsciiDoc:HEADING_TOKEN ('== Heading \\{esc} word')");
  }

  public void testAttributeWithoutValue() {
    doTest(":attribute:",
      """
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attribute')
        AsciiDoc:ATTRIBUTE_NAME_END (':')""");
  }

  public void testAttributeEmptyAtEnd() {
    doTest(":attribute!:",
      """
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attribute')
        AsciiDoc:ATTRIBUTE_UNSET ('!')
        AsciiDoc:ATTRIBUTE_NAME_END (':')""");
  }

  public void testAttributeWithSoftSet() {
    doTest(":attribute@:",
      """
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attribute')
        AsciiDoc:ATTRIBUTE_SOFTSET ('@')
        AsciiDoc:ATTRIBUTE_NAME_END (':')""");
  }

  public void testAttributeWithBlanks() {
    doTest(":attri b ute :",
      """
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attri b ute ')
        AsciiDoc:ATTRIBUTE_NAME_END (':')""");
  }


  public void testAttributeEmptyAtStart() {
    doTest(":attribute!:",
      """
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attribute')
        AsciiDoc:ATTRIBUTE_UNSET ('!')
        AsciiDoc:ATTRIBUTE_NAME_END (':')""");
  }

  public void testAttributeWithBracket() {
    doTest(":attr: icon:check[]",
      """
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attr')
        AsciiDoc:ATTRIBUTE_NAME_END (':')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ATTRIBUTE_VAL ('icon:check[]')""");
  }

  public void testAttributeInTitle() {
    doTest(".xx{hi}xx",
      """
        AsciiDoc:TITLE_TOKEN ('.')
        AsciiDoc:TEXT ('xx')
        AsciiDoc:ATTRIBUTE_REF_START ('{')
        AsciiDoc:ATTRIBUTE_REF ('hi')
        AsciiDoc:ATTRIBUTE_REF_END ('}')
        AsciiDoc:TEXT ('xx')""");
  }

  public void testBracketInBlockAttributes() {
    doTest("[val=\"{attr}[xx]\"]",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('val')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:ATTRIBUTE_REF_START ('{')
        AsciiDoc:ATTRIBUTE_REF ('attr')
        AsciiDoc:ATTRIBUTE_REF_END ('}')
        AsciiDoc:ATTR_VALUE ('[xx]')
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testAttributeEscaped() {
    doTest("\\:attribute:",
      "AsciiDoc:TEXT ('\\:attribute:')");
  }

  public void testAttributeWithValue() {
    doTest(":attribute: value",
      """
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attribute')
        AsciiDoc:ATTRIBUTE_NAME_END (':')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ATTRIBUTE_VAL ('value')""");
  }

  public void testAttributeWithNestedAttributeAndValue() {
    doTest(":attribute: {otherattr}value",
      """
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attribute')
        AsciiDoc:ATTRIBUTE_NAME_END (':')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ATTRIBUTE_REF_START ('{')
        AsciiDoc:ATTRIBUTE_REF ('otherattr')
        AsciiDoc:ATTRIBUTE_REF_END ('}')
        AsciiDoc:ATTRIBUTE_VAL ('value')""");
  }

  /**
   * Value continue on the next line if the line is ended by a space followed by a backslash.
   */
  public void testAttributeMultiline() {
    doTest(":attribute: value \\\n continue on the next line\nMore text",
      """
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attribute')
        AsciiDoc:ATTRIBUTE_NAME_END (':')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ATTRIBUTE_VAL ('value')
        AsciiDoc:ATTRIBUTE_CONTINUATION (' \\\\n ')
        AsciiDoc:ATTRIBUTE_VAL ('continue on the next line')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('More')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')""");
  }

  /**
   * Value continue on the next line if the line is ended by a space followed by a backslash.
   */
  public void testAttributeMultilineWithPlus() {
    //noinspection AsciiDocAttributeContinuationInspection
    doTest(":attribute: value +\n continue on the next line\nMore text",
      """
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attribute')
        AsciiDoc:ATTRIBUTE_NAME_END (':')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ATTRIBUTE_VAL ('value')
        AsciiDoc:ATTRIBUTE_CONTINUATION_LEGACY (' +\\n ')
        AsciiDoc:ATTRIBUTE_VAL ('continue on the next line')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('More')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')""");
  }

  public void testTwoConsecutiveAttributes() {
    doTest("Text\n\n:attribute1:\n:attribute2:",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attribute1')
        AsciiDoc:ATTRIBUTE_NAME_END (':')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('attribute2')
        AsciiDoc:ATTRIBUTE_NAME_END (':')""");
  }

  public void testNoAttributeAfterText() {
    doTest("Text\n:attribute:\n",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT (':attribute')
        AsciiDoc:END_OF_SENTENCE (':')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testContinuation() {
    doTest("+\n--\n",
      """
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('--')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testContinuationInList() {
    doTest("""
        * Hi
        +
        image::animage.png[]""",
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Hi')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_MACRO_ID ('image::')
        AsciiDoc:BLOCK_MACRO_BODY ('animage.png')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testContinuationsThatLookLikeAHeading() {
    doTest("""
        1. one
        +
        to
        +
        2. two""",
      """
        AsciiDoc:ENUMERATION ('1.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('one')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('to')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:ENUMERATION ('2.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('two')""");
  }

  public void testContinuationAfter() {
    doTest("--\n+\n",
      """
        AsciiDoc:BLOCK_DELIMITER ('--')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testContinuationThenTitle() {
    doTest("+\n.Title",
      """
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TITLE_TOKEN ('.')
        AsciiDoc:TEXT ('Title')""");
  }

  public void testAnchorThenTitle() {
    doTest("[#anchor]\n.Title",
      """
        AsciiDoc:BLOCKIDSTART ('[#')
        AsciiDoc:BLOCKID ('anchor')
        AsciiDoc:BLOCKIDEND (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TITLE_TOKEN ('.')
        AsciiDoc:TEXT ('Title')""");
  }

  public void testAnchorWithStyle() {
    doTest("[#anchor%style]\nText",
      """
        AsciiDoc:BLOCKIDSTART ('[#')
        AsciiDoc:BLOCKID ('anchor')
        AsciiDoc:SEPARATOR ('%style')
        AsciiDoc:BLOCKIDEND (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testBoldSimple() {
    doTest("Hello *bold* world",
      """
        AsciiDoc:TEXT ('Hello')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:BOLD_START ('*')
        AsciiDoc:BOLD ('bold')
        AsciiDoc:BOLD_END ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('world')""");
  }

  public void testBoldDouble() {
    doTest("Hello **bold** world",
      """
        AsciiDoc:TEXT ('Hello')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:DOUBLEBOLD_START ('**')
        AsciiDoc:BOLD ('bold')
        AsciiDoc:DOUBLEBOLD_END ('**')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('world')""");
  }

  public void testBoldDoubleMultiple() {
    doTest("**E**quivalent **M**odulo",
      """
        AsciiDoc:DOUBLEBOLD_START ('**')
        AsciiDoc:BOLD ('E')
        AsciiDoc:DOUBLEBOLD_END ('**')
        AsciiDoc:TEXT ('quivalent')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:DOUBLEBOLD_START ('**')
        AsciiDoc:BOLD ('M')
        AsciiDoc:DOUBLEBOLD_END ('**')
        AsciiDoc:TEXT ('odulo')""");
  }

  public void testItalicDoubleMultiple() {
    doTest("__E__quivalent __M__odulo",
      """
        AsciiDoc:DOUBLEITALIC_START ('__')
        AsciiDoc:ITALIC ('E')
        AsciiDoc:DOUBLEITALIC_END ('__')
        AsciiDoc:TEXT ('quivalent')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:DOUBLEITALIC_START ('__')
        AsciiDoc:ITALIC ('M')
        AsciiDoc:DOUBLEITALIC_END ('__')
        AsciiDoc:TEXT ('odulo')""");
  }

  public void testNonBoldWithBlockBreak() {
    doTest("Hello **bold\n\n** world",
      """
        AsciiDoc:TEXT ('Hello')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('**bold')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:BULLET ('**')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('world')""");
  }

  public void testBoldAtBeginningAndEndOfLineSingle() {
    doTest("*bold*",
      """
        AsciiDoc:BOLD_START ('*')
        AsciiDoc:BOLD ('bold')
        AsciiDoc:BOLD_END ('*')""");
  }

  public void testSingleQuote() {
    doTest("'single'",
      """
        AsciiDoc:SINGLE_QUOTE (''')
        AsciiDoc:TEXT ('single')
        AsciiDoc:SINGLE_QUOTE (''')""");
  }

  public void testNoSingleQuoteJustText() {
    doTest("don't",
      "AsciiDoc:TEXT ('don't')");
  }

  public void testItalicBlankAtEndOfFirstLine() {
    doTest("_test \ntest_",
      """
        AsciiDoc:ITALIC_START ('_')
        AsciiDoc:ITALIC ('test')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:ITALIC ('test')
        AsciiDoc:ITALIC_END ('_')""");
  }

  public void testNonItalicAsPrecededByNewline() {
    doTest("_test\n_",
      """
        AsciiDoc:TEXT ('_test')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('_')""");
  }

  public void testBoldMultipleInSingleLine() {
    doTest("bold *constrained* & **un**constrained",
      """
        AsciiDoc:TEXT ('bold')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:BOLD_START ('*')
        AsciiDoc:BOLD ('constrained')
        AsciiDoc:BOLD_END ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('&')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:DOUBLEBOLD_START ('**')
        AsciiDoc:BOLD ('un')
        AsciiDoc:DOUBLEBOLD_END ('**')
        AsciiDoc:TEXT ('constrained')""");
  }

  public void testConstrainedNumber() {
    doTest("11_11_11",
      "AsciiDoc:TEXT ('11_11_11')");
  }

  public void testSingleClosingQuoteInMonospace() {
    doTest("`test`'s formatting`",
      """
        AsciiDoc:MONO_START ('`')
        AsciiDoc:MONO ('test')
        AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_END ('`'')
        AsciiDoc:MONO ('s')
        AsciiDoc:WHITE_SPACE_MONO (' ')
        AsciiDoc:MONO ('formatting')
        AsciiDoc:MONO_END ('`')""");
  }

  public void testItalicMultipleInSingleLine() {
    doTest("italic _constrained_ & __un__constrained",
      """
        AsciiDoc:TEXT ('italic')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ITALIC_START ('_')
        AsciiDoc:ITALIC ('constrained')
        AsciiDoc:ITALIC_END ('_')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('&')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:DOUBLEITALIC_START ('__')
        AsciiDoc:ITALIC ('un')
        AsciiDoc:DOUBLEITALIC_END ('__')
        AsciiDoc:TEXT ('constrained')""");
  }

  public void testItalicWithTempltingUnderscore() {
    doTest("text `_t` text `_t`",
      """
        AsciiDoc:TEXT ('text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:MONO_START ('`')
        AsciiDoc:MONO ('_t')
        AsciiDoc:MONO_END ('`')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:MONO_START ('`')
        AsciiDoc:MONO ('_t')
        AsciiDoc:MONO_END ('`')""");
  }

  public void testItalicWithTwoTableCells() {
    doTest("|===\n|_text | text_\n|===",
      """
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('_text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text_')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|===')""");
  }

  public void testDoubleItalicWithTwoTableCells() {
    doTest("|===\n|__text | text__\n|===",
      """
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('__text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text__')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|===')""");
  }

  public void testWithItalicInTwoParagraphs() {
    doTest("_text_text\n\ntext_",
      """
        AsciiDoc:TEXT ('_text_text')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('text_')""");
  }

  public void testMonoMultipleInSingleLine() {
    doTest("mono `constrained` & ``un``constrained",
      """
        AsciiDoc:TEXT ('mono')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:MONO_START ('`')
        AsciiDoc:MONO ('constrained')
        AsciiDoc:MONO_END ('`')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('&')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:DOUBLEMONO_START ('``')
        AsciiDoc:MONO ('un')
        AsciiDoc:DOUBLEMONO_END ('``')
        AsciiDoc:TEXT ('constrained')""");
  }

  public void testMonoItalicBold() {
    doTest("``**__un__**``constrained",
      """
        AsciiDoc:DOUBLEMONO_START ('``')
        AsciiDoc:DOUBLEBOLD_START ('**')
        AsciiDoc:DOUBLEITALIC_START ('__')
        AsciiDoc:MONOBOLDITALIC ('un')
        AsciiDoc:DOUBLEITALIC_END ('__')
        AsciiDoc:DOUBLEBOLD_END ('**')
        AsciiDoc:DOUBLEMONO_END ('``')
        AsciiDoc:TEXT ('constrained')""");
  }

  public void testArrow() {
    doTest("-> => <- <=",
      """
        AsciiDoc:ARROW ('->')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ARROW ('=>')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ARROW ('<-')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ARROW ('<=')""");
  }

  public void testBoldAtBeginningAndEndOfLineDouble() {
    doTest("**bold**",
      """
        AsciiDoc:DOUBLEBOLD_START ('**')
        AsciiDoc:BOLD ('bold')
        AsciiDoc:DOUBLEBOLD_END ('**')""");
  }

  public void testNonMatchingBoldHead() {
    doTest("**bold*",
      """
        AsciiDoc:BOLD_START ('*')
        AsciiDoc:BOLD ('*bold')
        AsciiDoc:BOLD_END ('*')""");
  }

  public void testNonMatchingBoldTail() {
    doTest("*bold**",
      """
        AsciiDoc:BOLD_START ('*')
        AsciiDoc:BOLD ('bold*')
        AsciiDoc:BOLD_END ('*')""");
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
      """
        AsciiDoc:TEXT ('x')
        AsciiDoc:DOUBLEITALIC_START ('__')
        AsciiDoc:ITALIC ('*italiconly*')
        AsciiDoc:DOUBLEITALIC_END ('__')
        AsciiDoc:TEXT ('x')""");
  }

  public void testBoldItalic() {
    doTest("*_bolditalic_*",
      """
        AsciiDoc:BOLD_START ('*')
        AsciiDoc:ITALIC_START ('_')
        AsciiDoc:BOLDITALIC ('bolditalic')
        AsciiDoc:ITALIC_END ('_')
        AsciiDoc:BOLD_END ('*')""");
  }

  public void testConstrainedMustNotEndWithBlankBold() {
    doTest("*test * test*",
      """
        AsciiDoc:BOLD_START ('*')
        AsciiDoc:BOLD ('test')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:BOLD ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:BOLD ('test')
        AsciiDoc:BOLD_END ('*')""");
  }

  public void testConstrainedMustNotEndWithBlankItalic() {
    doTest("_test _ test_",
      """
        AsciiDoc:ITALIC_START ('_')
        AsciiDoc:ITALIC ('test')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ITALIC ('_')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ITALIC ('test')
        AsciiDoc:ITALIC_END ('_')""");
  }

  public void testConstrainedMustNotEndWithBlankMono() {
    doTest("`test ` test`",
      """
        AsciiDoc:MONO_START ('`')
        AsciiDoc:MONO ('test')
        AsciiDoc:WHITE_SPACE_MONO (' ')
        AsciiDoc:MONO ('`')
        AsciiDoc:WHITE_SPACE_MONO (' ')
        AsciiDoc:MONO ('test')
        AsciiDoc:MONO_END ('`')""");
  }

  public void testBullet() {
    doTest("* bullet",
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('bullet')""");
  }

  public void testBulletWithBlanksInFront() {
    doTest("  * bullet",
      """
        AsciiDoc:WHITE_SPACE ('  ')
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('bullet')""");
  }

  public void testMultipleBullets() {
    doTest("* bullet1\n* bullet2",
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('bullet1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('bullet2')""");
  }

  public void testMultipleBulletsLevel2() {
    doTest("** bullet1\n** bullet2",
      """
        AsciiDoc:BULLET ('**')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('bullet1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BULLET ('**')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('bullet2')""");
  }

  public void testThreeBulletItems() {
    doTest("""
        * abc
        * def
        * ghi
        """,
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('abc')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('def')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('ghi')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testSidebar() {
    doTest("****\nFoo\n****\n",
      """
        AsciiDoc:BLOCK_DELIMITER ('****')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Foo')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('****')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testRef() {
    doTest("Text <<REF>> More Text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:REFSTART ('<<')
        AsciiDoc:REF ('REF')
        AsciiDoc:REFEND ('>>')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('More')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testEscapedRef() {
    doTest("\\<<REF>>",
      """
        AsciiDoc:TEXT ('\\')
        AsciiDoc:LT ('<')
        AsciiDoc:LT ('<')
        AsciiDoc:TEXT ('REF')
        AsciiDoc:GT ('>')
        AsciiDoc:GT ('>')""");
  }

  public void testRefWithFile() {
    doTest("Text <<FILE#REF>> More Text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:REFSTART ('<<')
        AsciiDoc:REF ('FILE#REF')
        AsciiDoc:REFEND ('>>')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('More')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testRefWithRefText() {
    doTest("Text <<REF,Text>> More Text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:REFSTART ('<<')
        AsciiDoc:REF ('REF')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:REFTEXT ('Text')
        AsciiDoc:REFEND ('>>')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('More')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testRefWithRefTextWithClosingPointyBracket() {
    doTest("Text <<REF,Text > More>> Text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:REFSTART ('<<')
        AsciiDoc:REF ('REF')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:REFTEXT ('Text > More')
        AsciiDoc:REFEND ('>>')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testEscapedLink() {
    doTest("\\link:FILE[Text]",
      """
        AsciiDoc:TEXT ('\\link:FILE')
        AsciiDoc:LBRACKET ('[')
        AsciiDoc:TEXT ('Text')
        AsciiDoc:RBRACKET (']')""");
  }

  public void testEscapedLinkText() {
    doTest("link:FILE[T\\]ext]",
      """
        AsciiDoc:LINKSTART ('link:')
        AsciiDoc:LINKFILE ('FILE')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('T\\]ext')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testAttrInUrl() {
    doTest("http://url.com{path}[text]",
      """
        AsciiDoc:URL_LINK ('http://url.com')
        AsciiDoc:ATTRIBUTE_REF_START ('{')
        AsciiDoc:ATTRIBUTE_REF ('path')
        AsciiDoc:ATTRIBUTE_REF_END ('}')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('text')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testHtmlEntity() {
    doTest("Hi&amp;Ho",
      """
        AsciiDoc:TEXT ('Hi')
        AsciiDoc:HTML_ENTITY ('&amp;')
        AsciiDoc:TEXT ('Ho')""");
  }

  public void testUnicodeDecimalEntity() {
    doTest("Hi&#123;Ho",
      """
        AsciiDoc:TEXT ('Hi')
        AsciiDoc:HTML_ENTITY ('&#123;')
        AsciiDoc:TEXT ('Ho')""");
  }

  public void testUnicodeHexEntity() {
    doTest("Hi&#x123Af;Ho",
      """
        AsciiDoc:TEXT ('Hi')
        AsciiDoc:HTML_ENTITY ('&#x123Af;')
        AsciiDoc:TEXT ('Ho')""");
  }

  public void testHtmlEntityEscaped() {
    doTest("Hi\\&amp;Ho",
      "AsciiDoc:TEXT ('Hi\\&amp;Ho')");
  }

  public void testUnicodeDecimalEntityEscaped() {
    doTest("Hi\\&#123;Ho",
      "AsciiDoc:TEXT ('Hi\\&#123;Ho')");
  }

  public void testUnicodeHexEntityEscaped() {
    doTest("Hi\\&#x123Af;Ho",
      "AsciiDoc:TEXT ('Hi\\&#x123Af;Ho')");
  }

  public void testAttrInLink() {
    doTest("link:http://url.com{path}[text]",
      """
        AsciiDoc:LINKSTART ('link:')
        AsciiDoc:URL_LINK ('http://url.com')
        AsciiDoc:ATTRIBUTE_REF_START ('{')
        AsciiDoc:ATTRIBUTE_REF ('path')
        AsciiDoc:ATTRIBUTE_REF_END ('}')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('text')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testLinkWithAttributeAutocomplete() {
    doTest("link:IntellijIdeaRulezzz test.adoc[]\n",
      """
        AsciiDoc:LINKSTART ('link:')
        AsciiDoc:LINKFILE ('IntellijIdeaRulezzz test.adoc')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testLinkInFormatting() {
    doTest("`http://localhost:8080/`",
      """
        AsciiDoc:MONO_START ('`')
        AsciiDoc:URL_LINK ('http://localhost:8080/')
        AsciiDoc:MONO_END ('`')""");

  }

  public void testLinkCharLikeFormattingAtEnd() {
    doTest("http://localhost:8080/`",
      "AsciiDoc:URL_LINK ('http://localhost:8080/`')\n");
  }

  public void testLinkWithTitleAndContinuation() {
    doTest("link:test.adoc[Title +\nContinuing]\n",
      """
        AsciiDoc:LINKSTART ('link:')
        AsciiDoc:LINKFILE ('test.adoc')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('Title')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:MACROTEXT ('Continuing')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testLinkWithAnchor() {
    doTest("Text link:FILE#ANCHOR[Text] More Text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:LINKSTART ('link:')
        AsciiDoc:LINKFILE ('FILE')
        AsciiDoc:SEPARATOR ('#')
        AsciiDoc:LINKANCHOR ('ANCHOR')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('Text')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('More')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testLinkWithQuotes() {
    doTest("Text link:++https://example.org/?q=[a b]++[URL with special characters] Text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:LINKSTART ('link:')
        AsciiDoc:URL_LINK ('++https://example.org/?q=[a b]++')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('URL')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:MACROTEXT ('with')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:MACROTEXT ('special')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:MACROTEXT ('characters')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testLinkForAutocomplete() {
    doTest("Text link:FILEIntellijIdeaRulezzz More Text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:LINKSTART ('link:')
        AsciiDoc:LINKFILE ('FILEIntellijIdeaRulezzz More')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testLinkForAutocompleteWithBrackets() {
    doTest("link:IntellijIdeaRulezzz []",
      """
        AsciiDoc:LINKSTART ('link:')
        AsciiDoc:LINKFILE ('IntellijIdeaRulezzz ')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testBlockid() {
    doTest("[[BLOCKID]] Text",
      """
        AsciiDoc:BLOCKIDSTART ('[[')
        AsciiDoc:BLOCKID ('BLOCKID')
        AsciiDoc:BLOCKIDEND (']]')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testBlockidWithRefText() {
    doTest("[[BLOCKID,name]] Text",
      """
        AsciiDoc:BLOCKIDSTART ('[[')
        AsciiDoc:BLOCKID ('BLOCKID')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:BLOCKREFTEXT ('name')
        AsciiDoc:BLOCKIDEND (']]')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testAnchorid() {
    doTest("[#BLOCKID]Text",
      """
        AsciiDoc:BLOCKIDSTART ('[#')
        AsciiDoc:BLOCKID ('BLOCKID')
        AsciiDoc:BLOCKIDEND (']')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testAnchoridWithStyle() {
    doTest("[#BLOCKID]Text",
      """
        AsciiDoc:BLOCKIDSTART ('[#')
        AsciiDoc:BLOCKID ('BLOCKID')
        AsciiDoc:BLOCKIDEND (']')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testAnchorWithRefText() {
    doTest("[#BLOCKID,name]Text",
      """
        AsciiDoc:BLOCKIDSTART ('[#')
        AsciiDoc:BLOCKID ('BLOCKID')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:BLOCKREFTEXT ('name')
        AsciiDoc:BLOCKIDEND (']')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testAnchorWithClass() {
    doTest("[#BLOCKID.class]Text",
      """
        AsciiDoc:BLOCKIDSTART ('[#')
        AsciiDoc:BLOCKID ('BLOCKID')
        AsciiDoc:SEPARATOR ('.')
        AsciiDoc:BLOCKREFTEXT ('class')
        AsciiDoc:BLOCKIDEND (']')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testEscapedBold() {
    doTest("Text \\*nonbold* Text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('\\*nonbold*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testTypographicDoubleQuotes() {
    doTest("\"`typoquote`\"",
      """
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('"`')
        AsciiDoc:TEXT ('typoquote')
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`"')""");
  }

  public void testTypographicSingleQuotes() {
    doTest("'`typoquote`'",
      """
        AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_START (''`')
        AsciiDoc:TEXT ('typoquote')
        AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_END ('`'')""");
  }

  public void testMultipleDoubleTypographicQuotes() {
    doTest("\"`test?`\" \"`test?`\"",
      """
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('"`')
        AsciiDoc:TEXT ('test?')
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`"')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('"`')
        AsciiDoc:TEXT ('test?')
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`"')""");
  }

  public void testMultiplSingleTypographicQuotes() {
    doTest("'`test?`' '`test?`'",
      """
        AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_START (''`')
        AsciiDoc:TEXT ('test?')
        AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_END ('`'')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_START (''`')
        AsciiDoc:TEXT ('test?')
        AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_END ('`'')""");
  }

  public void testMonospaceWithQuotes() {
    doTest("`\"initial value\"`",
      """
        AsciiDoc:MONO_START ('`')
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:MONO ('initial')
        AsciiDoc:WHITE_SPACE_MONO (' ')
        AsciiDoc:MONO ('value')
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:MONO_END ('`')""");
  }

  public void testNoTypographicQuotes() {
    doTest("\"` test `\"",
      """
        AsciiDoc:DOUBLE_QUOTE ('"')
        AsciiDoc:TEXT ('`')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('test')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('`')
        AsciiDoc:DOUBLE_QUOTE ('"')""");
  }

  public void testTwoTypographicQuotesThatMightBeConsideredAMonospace() {
    doTest("\"`Test?`\", and \"`What?`\"",
      """
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('"`')
        AsciiDoc:TEXT ('Test?')
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`"')
        AsciiDoc:TEXT (',')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('and')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('"`')
        AsciiDoc:TEXT ('What?')
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`"')""");
  }

  public void testNoTypographicQuotesNonMatching() {
    doTest("\"`test",
      "AsciiDoc:DOUBLE_QUOTE ('\"')\n" +
        "AsciiDoc:TEXT ('`test')");
  }

  public void testPassThroughWithSomethingLookingLikeAnAttribute() {
    doTest("pass:[{attr}]",
      """
        AsciiDoc:INLINE_MACRO_ID ('pass:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('{attr}')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testPassWithEscapedContent() {
    doTest("pass:[\\]]",
      """
        AsciiDoc:INLINE_MACRO_ID ('pass:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('\\]')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testStemWithSomethingLookingLikeAnAttribute() {
    doTest("stem:[{attr}]",
      """
        AsciiDoc:INLINE_MACRO_ID ('stem:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('{attr}')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }


  public void testPassThroughInlineThreePlus() {
    doTest("+++pt\npt2+++",
      """
        AsciiDoc:PASSTRHOUGH_INLINE_START ('+++')
        AsciiDoc:PASSTRHOUGH_CONTENT ('pt\\npt2')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('+++')""");
  }

  public void testPassThroughInlineThreePlusWithPlusInside() {
    doTest("+++ ++ +++",
      """
        AsciiDoc:PASSTRHOUGH_INLINE_START ('+++')
        AsciiDoc:PASSTRHOUGH_CONTENT (' ++ ')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('+++')""");
  }

  public void testSuperscriptSimple() {
    doTest("^super^",
      """
        AsciiDoc:SUPERSCRIPT_START ('^')
        AsciiDoc:TEXT ('super')
        AsciiDoc:SUPERSCRIPT_END ('^')""");
  }

  public void testSuperscriptInside() {
    doTest("outside^inside^outside",
      """
        AsciiDoc:TEXT ('outside')
        AsciiDoc:SUPERSCRIPT_START ('^')
        AsciiDoc:TEXT ('inside')
        AsciiDoc:SUPERSCRIPT_END ('^')
        AsciiDoc:TEXT ('outside')""");
  }

  public void testNotSuperscriptStart() {
    doTest("^ inside^",
      """
        AsciiDoc:TEXT ('^')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('inside^')""");
  }

  public void testNotSuperscriptEnd() {
    doTest("^inside ^",
      """
        AsciiDoc:TEXT ('^inside')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('^')""");
  }

  public void testSubscriptSimple() {
    doTest("~sub~",
      """
        AsciiDoc:SUBSCRIPT_START ('~')
        AsciiDoc:TEXT ('sub')
        AsciiDoc:SUBSCRIPT_END ('~')""");
  }

  public void testSubscriptInside() {
    doTest("outside~inside~outside",
      """
        AsciiDoc:TEXT ('outside')
        AsciiDoc:SUBSCRIPT_START ('~')
        AsciiDoc:TEXT ('inside')
        AsciiDoc:SUBSCRIPT_END ('~')
        AsciiDoc:TEXT ('outside')""");
  }

  public void testNotSubscriptStart() {
    doTest("~ inside~",
      """
        AsciiDoc:TEXT ('~')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('inside~')""");
  }

  public void testNotSubscriptEnd() {
    doTest("~inside ~",
      """
        AsciiDoc:TEXT ('~inside')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('~')""");
  }

  public void testPassThroughInlineDollars() {
    doTest("$$pt\npt2$$",
      """
        AsciiDoc:PASSTRHOUGH_INLINE_START ('$$')
        AsciiDoc:PASSTRHOUGH_CONTENT ('pt\\npt2')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('$$')""");
  }
  public void testPassThroughInlineOnePlus() {
    doTest("+pt\np+t2+",
      """
        AsciiDoc:PASSTRHOUGH_INLINE_START ('+')
        AsciiDoc:PASSTRHOUGH_CONTENT ('pt\\np+t2')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('+')""");
  }

  public void testPassThroughInlineTwoPlus() {
    doTest("++pt\npt2++",
      """
        AsciiDoc:PASSTRHOUGH_INLINE_START ('++')
        AsciiDoc:PASSTRHOUGH_CONTENT ('pt\\npt2')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('++')""");
  }

  public void testPassThroughInlineTwoPlusEscaped() {
    doTest("\\++npt++",
      """
        AsciiDoc:TEXT ('\\+')
        AsciiDoc:PASSTRHOUGH_INLINE_START ('+')
        AsciiDoc:PASSTRHOUGH_CONTENT ('npt')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('+')
        AsciiDoc:TEXT ('+')""");
  }

  public void testPassThroughDoublePlusAndSingle() {
    doTest("++text++and +some+ other",
      """
        AsciiDoc:PASSTRHOUGH_INLINE_START ('++')
        AsciiDoc:PASSTRHOUGH_CONTENT ('text')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('++')
        AsciiDoc:TEXT ('and')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:PASSTRHOUGH_INLINE_START ('+')
        AsciiDoc:PASSTRHOUGH_CONTENT ('some')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('+')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('other')""");
  }

  public void testLiteralBlock() {
    doTest("....\nliteral\n....\n",
      """
        AsciiDoc:LITERAL_BLOCK_DELIMITER ('....')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LITERAL_BLOCK ('literal')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LITERAL_BLOCK_DELIMITER ('....')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testQuotedBlock() {
    doTest("____\nQuoted with *bold*\n____\n",
      """
        AsciiDoc:BLOCK_DELIMITER ('____')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Quoted')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('with')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:BOLD_START ('*')
        AsciiDoc:BOLD ('bold')
        AsciiDoc:BOLD_END ('*')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('____')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testNestedList() {
    doTest("""
        * item
        ** item

        == Section""",
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('item')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BULLET ('**')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('item')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:HEADING_TOKEN ('== Section')""");
  }

  public void testNestedQuotedBlock() {
    doTest("____\nQuoted\n_____\nDoubleQuote\n_____\n____\n",
      """
        AsciiDoc:BLOCK_DELIMITER ('____')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Quoted')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('_____')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('DoubleQuote')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('_____')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('____')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testListingNestedInExample() {
    doTest("====\n----\n----\n====\n",
      """
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testListingWithCallout() {
    doTest("----\n----\n<1> Callout 1\n<.> Callout 2",
      """
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CALLOUT ('<1>')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Callout')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CALLOUT ('<.>')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Callout')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('2')""");
  }

  public void testNoCallout() {
    doTest("Test\n" +
      "<1> Test", """
      AsciiDoc:TEXT ('Test')
      AsciiDoc:LINE_BREAK ('\\n')
      AsciiDoc:LT ('<')
      AsciiDoc:TEXT ('1')
      AsciiDoc:GT ('>')
      AsciiDoc:WHITE_SPACE (' ')
      AsciiDoc:TEXT ('Test')""");
  }

  public void testNoTitle() {
    doTest("text\n.notitle",
      """
        AsciiDoc:TEXT ('text')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('.notitle')""");
  }

  public void testTitleThatLooksLikeATableCell() {
    doTest(".|notatable",
      "AsciiDoc:TITLE_TOKEN ('.')\n" +
        "AsciiDoc:TEXT ('|notatable')");
  }

  public void testTitleAfterId() {
    doTest("[[id]]\n.Title\n====\nExample\n====",
      """
        AsciiDoc:BLOCKIDSTART ('[[')
        AsciiDoc:BLOCKID ('id')
        AsciiDoc:BLOCKIDEND (']]')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TITLE_TOKEN ('.')
        AsciiDoc:TEXT ('Title')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Example')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')""");
  }

  public void testTitleSTartingWithADot() {
    doTest("..gitignore\n----\nExample\n----",
      """
        AsciiDoc:TITLE_TOKEN ('.')
        AsciiDoc:TEXT ('.gitignore')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('Example')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')""");
  }

  public void testDoubleColonNotEndOfSentence() {
    doTest("::\n",
      "AsciiDoc:TEXT ('::')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testInitialNotEndOfSentenceMiddleOfLine() {
    doTest("Wolfgang A. Mozart",
      """
        AsciiDoc:TEXT ('Wolfgang')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('A.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Mozart')""");
  }

  public void testHardBreakWithContinuation() {
    doTest("* Test +\n+\nsecond line",
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Test')
        AsciiDoc:HARD_BREAK (' +')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('second')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('line')""");
  }

  public void testHardBreakAtBlockEnd() {
    doTest("""
        |===
        |XX +
        |===

        == Title""",
      """
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CELLSEPARATOR ('|')
        AsciiDoc:TEXT ('XX')
        AsciiDoc:HARD_BREAK (' +')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('|===')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:HEADING_TOKEN ('== Title')""");
  }

  public void testInitialEndOfSentenceAtEndOfLineSoThatItKeepsExistingWraps() {
    doTest("Wolfgang A.\nMozart",
      """
        AsciiDoc:TEXT ('Wolfgang')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('A')
        AsciiDoc:END_OF_SENTENCE ('.')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Mozart')""");
  }

  public void testDontWrapIfFollowedByNumberInsideLine() {
    doTest("Ch. 9 important",
      """
        AsciiDoc:TEXT ('Ch.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('9')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('important')""");
  }

  public void testDontWrapWhenNumberWithDotInsideLine() {
    doTest("CSS3. Text",
      """
        AsciiDoc:TEXT ('CSS3.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testKeepWrapWithNumberAndDotAtEndOfLine() {
    doTest("CSS3.\nText",
      """
        AsciiDoc:TEXT ('CSS3')
        AsciiDoc:END_OF_SENTENCE ('.')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testDonWrapIfFollowedByNumberNextLineLine() {
    doTest("Ch.\n9 important",
      """
        AsciiDoc:TEXT ('Ch')
        AsciiDoc:END_OF_SENTENCE ('.')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('9')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('important')""");
  }

  public void testInitialEndOfSentenceAtEndOfLineSoThatItKeepsExistingWrapsEvenIfThereIsABlankAtTheEndOfTheLine() {
    doTest("Wolfgang A. \nMozart",
      """
        AsciiDoc:TEXT ('Wolfgang')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('A')
        AsciiDoc:END_OF_SENTENCE ('.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Mozart')""");
  }

  public void testExampleWithBlankLine() {
    doTest("====\nTest\n\n====\n",
      """
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Test')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testExampleWithListing() {
    doTest("====\n.Title\n[source]\n----\nSource\n----\n====\n",
      """
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TITLE_TOKEN ('.')
        AsciiDoc:TEXT ('Title')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('source')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('Source')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testBlockEndingOverOldStyleHeader() {
    doTest("--\nS\n--\n",
      """
        AsciiDoc:BLOCK_DELIMITER ('--')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('S')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('--')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testContinuationWithBlockAndEnumeration() {
    doTest("""
        * Test
        +
        --
        * Test
        --

        == Section
        """,
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Test')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('--')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Test')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('--')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:HEADING_TOKEN ('== Section')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testHorizontalRule() {
    doTest("'''\n",
      "AsciiDoc:HORIZONTALRULE (''''')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testMarkdownHorizontalRuleDash() {
    //noinspection AsciiDocHorizontalRuleInspection
    doTest("---\n",
      "AsciiDoc:HORIZONTALRULE ('---')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testMarkdownHorizontalRuleStar() {
    //noinspection AsciiDocHorizontalRuleInspection
    doTest("***\n",
      "AsciiDoc:HORIZONTALRULE ('***')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testMarkdownHorizontalRuleUnderscore() {
    //noinspection AsciiDocHorizontalRuleInspection
    doTest("___\n",
      "AsciiDoc:HORIZONTALRULE ('___')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testPagebreak() {
    doTest("<<<\n",
      "AsciiDoc:PAGEBREAK ('<<<')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testPagebreakWithAutocomplete() {
    doTest("<<<IntellijIdeaRulezzz\n",
            "AsciiDoc:PAGEBREAK ('<<<IntellijIdeaRulezzz')\n" +
                    "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testEscapedBlockId() {
    doTest("\\[[id]]",
      """
        AsciiDoc:TEXT ('\\')
        AsciiDoc:LBRACKET ('[')
        AsciiDoc:LBRACKET ('[')
        AsciiDoc:TEXT ('id')
        AsciiDoc:RBRACKET (']')
        AsciiDoc:RBRACKET (']')""");
  }

  public void testEndOfSentence() {
    doTest("End. Of Sentence",
      """
        AsciiDoc:TEXT ('End')
        AsciiDoc:END_OF_SENTENCE ('.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Of')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Sentence')""");
  }

  public void testEndOfSentenceWithUmlaut() {
    doTest("End. Öf Sentence",
      """
        AsciiDoc:TEXT ('End')
        AsciiDoc:END_OF_SENTENCE ('.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Öf')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Sentence')""");
  }

  public void testNoEndOfSentence() {
    doTest("End.No Sentence",
      """
        AsciiDoc:TEXT ('End.No')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Sentence')""");
  }

  public void testNoEndOfSentenceAfterNumber() {
    doTest("After 1. Number",
      """
        AsciiDoc:TEXT ('After')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('1.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Number')""");
  }

  public void testNoEndOfSentenceAfterColon() {
    doTest("Colon: Word",
      """
        AsciiDoc:TEXT ('Colon:')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Word')""");
  }

  public void testEndOfSentenceAfterColonAndNewline() {
    doTest("Colon:\nWord",
      """
        AsciiDoc:TEXT ('Colon')
        AsciiDoc:END_OF_SENTENCE (':')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Word')""");
  }

  public void testNoEndOfSentenceAgain() {
    doTest("End. no Sentence",
      """
        AsciiDoc:TEXT ('End.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('no')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Sentence')""");
  }

  public void testNoEndOfSentenceAdExemplar() {
    doTest("e.g. No Sentence",
      """
        AsciiDoc:TEXT ('e.g.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('No')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Sentence')""");
  }

  public void testDescription() {
    doTest("a property:: description",
      """
        AsciiDoc:DESCRIPTION ('a')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:DESCRIPTION ('property')
        AsciiDoc:DESCRIPTION_END ('::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('description')""");
  }

  public void testCommentForDescription() {
    doTest("// a property:: description",
      "AsciiDoc:LINE_COMMENT ('// a property:: description')");
  }

  public void testDescriptionLong() {
    doTest("A:: B\nC::: D",
      """
        AsciiDoc:DESCRIPTION ('A')
        AsciiDoc:DESCRIPTION_END ('::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('B')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:DESCRIPTION ('C')
        AsciiDoc:DESCRIPTION_END (':::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('D')""");
  }

  public void testDescriptionWithContinuationAndListing() {
    doTest("""
        X:: Y
        +
        ----
        ----
        == Hi""",
      """
        AsciiDoc:DESCRIPTION ('X')
        AsciiDoc:DESCRIPTION_END ('::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Y')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('==')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Hi')""");
  }

  public void testDescriptionWithContinuationAndExample() {
    doTest("""
        X:: Y
        +
        ====
        ====
        == Hi""",
      """
        AsciiDoc:DESCRIPTION ('X')
        AsciiDoc:DESCRIPTION_END ('::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Y')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('==')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Hi')""");
  }

  public void testHeadingAfterListing() {
    doTest("""
        ----
        ----
        == Hi""",
      """
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:HEADING_TOKEN ('== Hi')""");
  }

  public void testDescriptionWithMultipleColons() {
    doTest("a property::ext:: description",
      """
        AsciiDoc:DESCRIPTION ('a')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:DESCRIPTION ('property::ext')
        AsciiDoc:DESCRIPTION_END ('::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('description')""");
  }

  public void testDescriptionWithFormatting() {
    doTest("`property`:: description",
      """
        AsciiDoc:MONO_START ('`')
        AsciiDoc:DESCRIPTION ('property')
        AsciiDoc:MONO_END ('`')
        AsciiDoc:DESCRIPTION_END ('::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('description')""");
  }

  public void testDescriptionWithLink() {
    doTest("link:http://www.example.com[Example]:: description",
      """
        AsciiDoc:LINKSTART ('link:')
        AsciiDoc:URL_LINK ('http://www.example.com')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('Example')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:DESCRIPTION_END ('::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('description')""");
  }

  public void testDescriptionWithAttribute() {
    doTest("{attr}:: description",
      """
        AsciiDoc:ATTRIBUTE_REF_START ('{')
        AsciiDoc:ATTRIBUTE_REF ('attr')
        AsciiDoc:ATTRIBUTE_REF_END ('}')
        AsciiDoc:DESCRIPTION_END ('::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('description')""");
  }

  public void testIndentedListing() {
    doTest("   Listing\nMore\n\nText",
      """
        AsciiDoc:LISTING_TEXT ('   Listing')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('More')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testListingWithNoDelimiters() {
    doTest("[source]\nListing\n\nText",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('source')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('Listing')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testListingNoStyle() {
    doTest("  Listing\n+\nText",
      """
        AsciiDoc:LISTING_TEXT ('  Listing')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:CONTINUATION ('+')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testListingWithLanguage() {
    doTest("[source,php]\nListing\n\nText",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('source')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:ATTR_NAME ('php')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('Listing')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testListingWithLanguageButNoSource() {
    doTest("[,php]\nListing\n\nText",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:ATTR_NAME ('php')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('Listing')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testListingWithRef() {
    doTest("[source#ref]\nListing\n\nText",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('source')
        AsciiDoc:SEPARATOR ('#')
        AsciiDoc:BLOCKID ('ref')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('Listing')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testListingWithOpenBlock() {
    doTest("[source]\n--\nListing\n--\nText",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('source')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('--')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('Listing')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('--')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testPassthroughWithNoDelimiters() {
    doTest("[pass]\nPas**ss**ss\n\nText",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('pass')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:PASSTRHOUGH_CONTENT ('Pas**ss**ss')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testListingWithAttributeAndDelimiter() {
    doTest("[source]\n----\nListing\n----\nText",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('source')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('Listing')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testVerseWithCommentNoDelimiters() {
    doTest("""
        [verse]
        // test
         Verse
        """,
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('verse')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LINE_COMMENT ('// test')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Verse')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testBlockWithTitleInsideExample() {
    doTest("""
        ====
        Text

        .Title
        ----
        Hi
        ----
        ====""",
      """
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Text')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TITLE_TOKEN ('.')
        AsciiDoc:TEXT ('Title')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('Hi')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')""");
  }

  public void testVerseWithSomethingLookingLikeBlock() {
    doTest("""
        [verse]
        V1
        ----
        V2

        [source]
        Hi""",
      """
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('verse')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('V1')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('V2')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('source')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT ('Hi')""");
  }

  public void testEnumeration() {
    doTest(". Item",
      """
        AsciiDoc:ENUMERATION ('.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Item')""");
  }

  public void testEnumerationWithBlank() {
    doTest("""
      * item 1
       * item 2
      """, """
      AsciiDoc:BULLET ('*')
      AsciiDoc:WHITE_SPACE (' ')
      AsciiDoc:TEXT ('item')
      AsciiDoc:WHITE_SPACE (' ')
      AsciiDoc:TEXT ('1')
      AsciiDoc:LINE_BREAK ('\\n')
      AsciiDoc:WHITE_SPACE (' ')
      AsciiDoc:BULLET ('*')
      AsciiDoc:WHITE_SPACE (' ')
      AsciiDoc:TEXT ('item')
      AsciiDoc:WHITE_SPACE (' ')
      AsciiDoc:TEXT ('2')
      AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testNoEnumeration() {
    doTest("Das\n" +
      "* Test", """
      AsciiDoc:TEXT ('Das')
      AsciiDoc:LINE_BREAK ('\\n')
      AsciiDoc:TEXT ('*')
      AsciiDoc:WHITE_SPACE (' ')
      AsciiDoc:TEXT ('Test')""");
  }

  public void testItemWithListing() {
    doTest("""
        * item
        ----
        ----""",
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('item')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_BLOCK_DELIMITER ('----')""");
  }

  public void testEnumerationSecondLevel() {
    doTest(".. Item",
      """
        AsciiDoc:ENUMERATION ('..')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Item')""");
  }

  public void testEnumerationNumber() {
    doTest("1. Item",
      """
        AsciiDoc:ENUMERATION ('1.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Item')""");
  }

  public void testEnumerationCharacter() {
    doTest("a. Item",
      """
        AsciiDoc:ENUMERATION ('a.')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Item')""");
  }

  public void testEndingBlockWithNoDelimiterInsideBlockWithDelimiter() {
    doTest("""
        ====
        [verse]
        test
        ----
        ====
        """,
      """
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('verse')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('test')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('----')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testInlineMacro() {
    doTest("Text image:image.png[] text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:INLINE_MACRO_ID ('image:')
        AsciiDoc:INLINE_MACRO_BODY ('image.png')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')""");
  }

  public void testInlineMacroWithBlank() {
    doTest("pass:[ blank ]",
      """
        AsciiDoc:INLINE_MACRO_ID ('pass:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:MACROTEXT ('blank')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testInlineMacroWithAttributeInBody() {
    doTest("image:{url}/image.png[]",
      """
        AsciiDoc:INLINE_MACRO_ID ('image:')
        AsciiDoc:ATTRIBUTE_REF_START ('{')
        AsciiDoc:ATTRIBUTE_REF ('url')
        AsciiDoc:ATTRIBUTE_REF_END ('}')
        AsciiDoc:INLINE_MACRO_BODY ('/image.png')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testInlineMacroWithPassthrough() {
    doTest("kbd:[+]+]",
      """
        AsciiDoc:INLINE_MACRO_ID ('kbd:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:PASSTRHOUGH_INLINE_START ('+')
        AsciiDoc:PASSTRHOUGH_CONTENT (']')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('+')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testInlineMacroWithPassthroughAndSpaces() {
    doTest("kbd:[X + ++ + ++]",
      """
        AsciiDoc:INLINE_MACRO_ID ('kbd:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('X')
        AsciiDoc:SEPARATOR (' + ')
        AsciiDoc:PASSTRHOUGH_INLINE_START ('++')
        AsciiDoc:PASSTRHOUGH_CONTENT (' + ')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('++')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testInlineMacroWithNoPassthrough() {
    doTest("kbd:[⌃+X+X]+",
      """
        AsciiDoc:INLINE_MACRO_ID ('kbd:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('⌃')
        AsciiDoc:SEPARATOR ('+')
        AsciiDoc:MACROTEXT ('X')
        AsciiDoc:SEPARATOR ('+')
        AsciiDoc:MACROTEXT ('X')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:TEXT ('+')""");
  }

  public void testInlineMacroWithTextAndPassthrough() {
    doTest("kbd:[text++]++]",
      """
        AsciiDoc:INLINE_MACRO_ID ('kbd:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('text')
        AsciiDoc:PASSTRHOUGH_INLINE_START ('++')
        AsciiDoc:PASSTRHOUGH_CONTENT (']')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('++')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testInlineMacroMultiLine() {
    doTest("image:image.png[Text\nText]",
      """
        AsciiDoc:INLINE_MACRO_ID ('image:')
        AsciiDoc:INLINE_MACRO_BODY ('image.png')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('Text')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:ATTR_NAME ('Text')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testInlineMacroWithAttribute() {
    doTest("Text image:image.png[link=http://www.gmx.net] text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:INLINE_MACRO_ID ('image:')
        AsciiDoc:INLINE_MACRO_BODY ('image.png')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('link')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')""");
  }

  public void testInlineMacroWithAttributeRef() {
    doTest("Text image:image.png[link={url}] text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:INLINE_MACRO_ID ('image:')
        AsciiDoc:INLINE_MACRO_BODY ('image.png')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('link')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:ATTRIBUTE_REF_START ('{')
        AsciiDoc:ATTRIBUTE_REF ('url')
        AsciiDoc:ATTRIBUTE_REF_END ('}')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')""");
  }

  public void testInlineMacroWithBracketsInside() {
    doTest("Text footnote:[some macro:text[About]] text",
      """
        AsciiDoc:TEXT ('Text')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:INLINE_MACRO_ID ('footnote:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('some')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:MACROTEXT ('macro:text[About]')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')""");
  }

  public void testFootnoteAdjacentToText() {
    doTest("prefootnote:[xx] xx",
      """
        AsciiDoc:TEXT ('pre')
        AsciiDoc:INLINE_MACRO_ID ('footnote:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('xx')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('xx')""");
  }

  public void testFootnoteAfterEscape() {
    doTest("\\escapedfootnote:[xx] xx",
      """
        AsciiDoc:TEXT ('\\escaped')
        AsciiDoc:INLINE_MACRO_ID ('footnote:')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('xx')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('xx')""");
  }

  public void testInlineMacroThatIsIncompleteAndHasAnInlineMacroOnTheSameLine() {
    doTest("weight:120kg label:procedure[]",
      """
        AsciiDoc:TEXT ('weight:120kg')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:INLINE_MACRO_ID ('label:')
        AsciiDoc:INLINE_MACRO_BODY ('procedure')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testInlineMacroThatSupportsBlanks() {
    doTest("xref:This has Blanks[]",
      """
        AsciiDoc:LINKSTART ('xref:')
        AsciiDoc:LINKFILE ('This has Blanks')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testInlineMacroThatHasAttributeWhenThereIsAnEqualSign() {
    doTest("xref:file.adoc[attr1=a,attr2=b]",
      """
        AsciiDoc:LINKSTART ('xref:')
        AsciiDoc:LINKFILE ('file.adoc')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('attr1')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:ATTR_VALUE ('a')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:ATTR_NAME ('attr2')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:ATTR_VALUE ('b')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testInlineMacroThatDoesntSupportBlanks() {
    doTest("label:This is a label[]",
      """
        AsciiDoc:TEXT ('label:This')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('is')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('a')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('label')
        AsciiDoc:LBRACKET ('[')
        AsciiDoc:RBRACKET (']')""");
  }

  public void testInlineMacroThatSupportFileNames() {
    doTest("image:file with blank.png[]",
      """
        AsciiDoc:INLINE_MACRO_ID ('image:')
        AsciiDoc:INLINE_MACRO_BODY ('file with blank.png')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testBlockMacroWithBracketsInside() {
    doTest("macro::text[other:[hi]]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('macro::')
        AsciiDoc:BLOCK_MACRO_BODY ('text')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('other:[hi]')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testBlockMacroWithTwoAttributes() {
    doTest("macro::text[1,2]\n\nText",
      """
        AsciiDoc:BLOCK_MACRO_ID ('macro::')
        AsciiDoc:BLOCK_MACRO_BODY ('text')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('1')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:ATTR_NAME ('2')
        AsciiDoc:ATTRS_END (']')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testBlockMacroAutocompleteWithOtherMacroInSameLine() {
    doTest("xref:" + CompletionUtilCore.DUMMY_IDENTIFIER + " and other xref:complete[] normal text",
      """
        AsciiDoc:LINKSTART ('xref:')
        AsciiDoc:LINKFILE ('IntellijIdeaRulezzz ')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('and')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('other')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:LINKSTART ('xref:')
        AsciiDoc:LINKFILE ('complete')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('normal')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')""");
  }

  public void testBlockMacroAutocompleteAtEndOfLine() {
    doTest("xref:" + CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED,
      "AsciiDoc:LINKSTART ('xref:')\n" +
        "AsciiDoc:LINKFILE ('IntellijIdeaRulezzz')");
  }

  public void testBlockMacroAutocompleteCompleteMacroWithOtherMacroInSameLine() {
    doTest("xref:" + CompletionUtilCore.DUMMY_IDENTIFIER + "complete[] and other xref:complete[] normal text",
      """
        AsciiDoc:LINKSTART ('xref:')
        AsciiDoc:LINKFILE ('IntellijIdeaRulezzz complete')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('and')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('other')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:LINKSTART ('xref:')
        AsciiDoc:LINKFILE ('complete')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('normal')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')""");
  }

  public void testBlockMacroAfterList() {
    doTest("""
      * Item

      blockmacro::content[]""", """
      AsciiDoc:BULLET ('*')
      AsciiDoc:WHITE_SPACE (' ')
      AsciiDoc:TEXT ('Item')
      AsciiDoc:LINE_BREAK ('\\n')
      AsciiDoc:EMPTY_LINE ('\\n')
      AsciiDoc:BLOCK_MACRO_ID ('blockmacro::')
      AsciiDoc:BLOCK_MACRO_BODY ('content')
      AsciiDoc:ATTRS_START ('[')
      AsciiDoc:ATTRS_END (']')""");
  }

  public void testExampleWithListingNoDelimiter() {
    doTest("""
        ====
         Test
        ====
        """,
      """
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:LISTING_TEXT (' Test')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:BLOCK_DELIMITER ('====')
        AsciiDoc:LINE_BREAK ('\\n')""");
  }

  public void testEllipseInsideLIne() {
    doTest("Text... Text",
      """
        AsciiDoc:TEXT ('Text...')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testResetFormatting() {
    doTest("`Mono Text++`++\n\nText",
      """
        AsciiDoc:MONO_START ('`')
        AsciiDoc:MONO ('Mono')
        AsciiDoc:WHITE_SPACE_MONO (' ')
        AsciiDoc:MONO ('Text')
        AsciiDoc:PASSTRHOUGH_INLINE_START ('++')
        AsciiDoc:PASSTRHOUGH_CONTENT ('`')
        AsciiDoc:PASSTRHOUGH_INLINE_END ('++')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:EMPTY_LINE ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testIfDef() {
    doTest("ifdef::attr[]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('ifdef::')
        AsciiDoc:ATTRIBUTE_REF ('attr')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testIfDefWithAttributeInBody() {
    doTest("ifdef::attr[:other: val]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('ifdef::')
        AsciiDoc:ATTRIBUTE_REF ('attr')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTRIBUTE_NAME_START (':')
        AsciiDoc:ATTRIBUTE_NAME ('other')
        AsciiDoc:ATTRIBUTE_NAME_END (':')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:ATTRIBUTE_VAL ('val')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testLinkAttribute() {
    doTest("image::file.png[link='http://www.gmx.net']",
      """
        AsciiDoc:BLOCK_MACRO_ID ('image::')
        AsciiDoc:BLOCK_MACRO_BODY ('file.png')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('link')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:SINGLE_QUOTE (''')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:SINGLE_QUOTE (''')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testEndifSpecial() {
    doTest("endif::[]",
      """
        AsciiDoc:BLOCK_MACRO_ID ('endif::')
        AsciiDoc:ATTRS_START ('[')
        AsciiDoc:ATTRS_END (']')""");
  }

  public void testSimpleUrl() {
    doTest("http://www.gmx.net",
      "AsciiDoc:URL_LINK ('http://www.gmx.net')");
  }

  public void testSimpleUrlWithAttributesInBrackets() {
    doTest("http://www.gmx.net[attr=val]",
      """
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:ATTR_NAME ('attr')
        AsciiDoc:ASSIGNMENT ('=')
        AsciiDoc:ATTR_VALUE ('val')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testSimpleUrlWithTextInBrackets() {
    doTest("http://www.gmx.net[text]",
      """
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('text')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testSimpleUrlAtEndOfSentence() {
    doTest("http://www.gmx.net.",
      "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:TEXT ('.')");
  }

  public void testSimpleUrlInParentheses() {
    doTest("(http://www.gmx.net)",
      """
        AsciiDoc:LPAREN ('(')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:RPAREN (')')""");
  }

  public void testSimpleUrlInParenthesesWithColon() {
    doTest("(http://www.gmx.net):",
      """
        AsciiDoc:LPAREN ('(')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:RPAREN (')')
        AsciiDoc:TEXT (':')""");
  }

  public void testSimpleUrlInParenthesesAndText() {
    doTest("(http://www.gmx.net) Text",
      """
        AsciiDoc:LPAREN ('(')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:RPAREN (')')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testSimpleUrlInParenthesesWithColonAndText() {
    doTest("(http://www.gmx.net): Text",
      """
        AsciiDoc:LPAREN ('(')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:RPAREN (')')
        AsciiDoc:TEXT (':')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testSimpleUrlInParenthesesAtEndOfLine() {
    doTest("(http://www.gmx.net)\nText",
      """
        AsciiDoc:LPAREN ('(')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:RPAREN (')')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testSimpleUrlInParenthesesWithColonAtEndOfLine() {
    doTest("(http://www.gmx.net):\nText",
      """
        AsciiDoc:LPAREN ('(')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:RPAREN (')')
        AsciiDoc:END_OF_SENTENCE (':')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Text')""");
  }

  public void testUrlInBrackets() {
    doTest("<http://www.gmx.net>",
      """
        AsciiDoc:URL_START ('<')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:URL_END ('>')""");
  }

  public void testUrlDescriptionList() {
    doTest("http://example.com:: test",
      """
        AsciiDoc:URL_LINK ('http://example.com')
        AsciiDoc:DESCRIPTION_END ('::')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('test')""");
  }

  public void testUrlNoDescriptionList() {
    doTest("text\nhttp://example.com:: test",
      """
        AsciiDoc:TEXT ('text')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:URL_LINK ('http://example.com:')
        AsciiDoc:TEXT (':')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('test')""");
  }

  public void testUrlInQuotes() {
    doTest("\"`http://www.gmx.net`\"",
      """
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('"`')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`"')""");
  }

  public void testUrlEscapedInMonoSpace() {
    doTest("`\\http://www.gmx.net` text",
      """
        AsciiDoc:MONO_START ('`')
        AsciiDoc:MONO ('\\http://www.gmx.net')
        AsciiDoc:MONO_END ('`')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('text')""");
  }

  public void testUrlInBracketsWithSpace() {
    doTest("<http://www.gmx.net >",
      """
        AsciiDoc:LT ('<')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:GT ('>')""");
  }

  public void testUrlInBracketsWithSquareBracket() {
    doTest("<http://www.gmx.net[Hi]>",
      """
        AsciiDoc:LT ('<')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('Hi')
        AsciiDoc:INLINE_ATTRS_END (']')
        AsciiDoc:GT ('>')""");
  }

  public void testUrlWithLinkPrefix() {
    doTest("link:http://www.gmx.net[Hi]",
      """
        AsciiDoc:LINKSTART ('link:')
        AsciiDoc:URL_LINK ('http://www.gmx.net')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:MACROTEXT ('Hi')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testEmail() {
    doTest("doc.writer@example.com",
      "AsciiDoc:URL_EMAIL ('doc.writer@example.com')");
  }

  public void testEmailWithPrefix() {
    doTest("mailto:doc.writer@example.com[]",
      """
        AsciiDoc:URL_PREFIX ('mailto:')
        AsciiDoc:URL_EMAIL ('doc.writer@example.com')
        AsciiDoc:INLINE_ATTRS_START ('[')
        AsciiDoc:INLINE_ATTRS_END (']')""");
  }

  public void testEmailWithPrefixButNoSquareBrackets() {
    doTest("mailto:doc.writer@example.com",
      "AsciiDoc:TEXT ('mailto:doc.writer@example.com')");
  }

  public void testFrontmatter() {
    doTest("---\nhi-hi: ho\n---",
      """
        AsciiDoc:FRONTMATTER_DELIMITER ('---')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:FRONTMATTER ('hi-hi: ho')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:FRONTMATTER_DELIMITER ('---')""");
  }

  public void testFrontmatterWithComment() {
    doTest("---\n# comment\n---",
      """
        AsciiDoc:FRONTMATTER_DELIMITER ('---')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:FRONTMATTER ('# comment')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:FRONTMATTER_DELIMITER ('---')""");
  }

  public void testBibliography() {
    doTest("* [[[bib,2]]] Book",
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:BIBSTART ('[[[')
        AsciiDoc:BLOCKID ('bib')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:BLOCKREFTEXT ('2')
        AsciiDoc:BIBEND (']]]')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('Book')""");
  }

  public void testBibliographyOnNewLine() {
    doTest("* [[[bib,2]]]\nBook",
      """
        AsciiDoc:BULLET ('*')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:BIBSTART ('[[[')
        AsciiDoc:BLOCKID ('bib')
        AsciiDoc:SEPARATOR (',')
        AsciiDoc:BLOCKREFTEXT ('2')
        AsciiDoc:BIBEND (']]]')
        AsciiDoc:LINE_BREAK ('\\n')
        AsciiDoc:TEXT ('Book')""");
  }

  public void testInlineId() {
    doTest("The [[id]] word",
      """
        AsciiDoc:TEXT ('The')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:INLINEIDSTART ('[[')
        AsciiDoc:BLOCKID ('id')
        AsciiDoc:INLINEIDEND (']]')
        AsciiDoc:WHITE_SPACE (' ')
        AsciiDoc:TEXT ('word')""");
  }

  @Override
  protected void doTest(@Language("asciidoc") @NonNls String text, @Nullable String expected) {
    super.doTest(text, expected);
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

package org.asciidoc.intellij.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.testFramework.LexerTestCase;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
@SuppressWarnings({"AsciiDocHeadingStyle", "AsciiDocLinkResolve", "AsciiDocAttributeContinuation", "AsciiDocReferenceResolve", "AsciiDocHorizontalRule"})
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

  public void testListingWithSpaceDash() {
    doTest("----\nbbbb\n---- --\ncccc\n----\nText",
      "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('bbbb')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('---- --')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('cccc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testTextLookingLikeListing() {
    doTest("---- --\nnolisting",
      "AsciiDoc:TEXT ('----')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('nolisting')");
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
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testIncludeWithTags() {
    doTest("include::file.adoc[tags=tag1;tag2]",
      "AsciiDoc:BLOCK_MACRO_ID ('include::')\n" +
        "AsciiDoc:BLOCK_MACRO_BODY ('file.adoc')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('tags')\n" +
        "AsciiDoc:ASSIGNMENT ('=')\n" +
        "AsciiDoc:ATTR_VALUE ('tag1')\n" +
        "AsciiDoc:ATTR_LIST_SEP (';')\n" +
        "AsciiDoc:ATTR_VALUE ('tag2')\n" +
        "AsciiDoc:ATTRS_END (']')");
  }

  public void testHeading() {
    doTest("= Abc\n\nabc\n== Def\ndef",
      "AsciiDoc:HEADING ('= Abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
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
    doTest("Abc\n===\n\ndef",
      "AsciiDoc:HEADING_OLDSTYLE ('Abc\\n===')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:TEXT ('def')");
  }

  public void testHeadingOldStyleWithHeaderSeparatedByBlankLine() {
    doTest("Abc\n===\nHeader\n\ndef",
      "AsciiDoc:HEADING_OLDSTYLE ('Abc\\n===')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:TEXT ('def')");
  }

  public void testHeadingOldStyleWithHeaderTwoLines() {
    doTest("Abc\n===\nHeader1\nHeader2\ndef",
      "AsciiDoc:HEADING_OLDSTYLE ('Abc\\n===')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header2')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('def')");
  }

  public void testHeadingNewStyleWithHeaderTwoLines() {
    doTest("= Abc\nHeader1\nHeader2\ndef",
      "AsciiDoc:HEADING ('= Abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header2')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('def')");
  }

  public void testHeadingNewStyleWithHeaderTwoLinesAndLineComment() {
    doTest("= Abc\nHeader1\n// Comment\nHeader2\ndef",
      "AsciiDoc:HEADING ('= Abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LINE_COMMENT ('// Comment')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header2')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('def')");
  }

  public void testHeadingNewStyleWithHeaderTwoLinesAndBlockComment() {
    doTest("= Abc\nHeader1\n////\nHiHo\n////\nHeader2\ndef",
      "AsciiDoc:HEADING ('= Abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_COMMENT ('////')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_COMMENT ('HiHo')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_COMMENT ('////')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header2')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('def')");
  }

  public void testHeadingNewStyleWithInclude() {
    doTest("= Abc\ninclude::test.adoc[]\n",
      "AsciiDoc:HEADING ('= Abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_MACRO_ID ('include::')\n" +
        "AsciiDoc:BLOCK_MACRO_BODY ('test.adoc')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testHeadingMarkdownStyleWithHeaderTwoLines() {
    //noinspection AsciiDocHeadingStyleInspection
    doTest("# Abc\nHeader1\nHeader2\ndef",
      "AsciiDoc:HEADING ('# Abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header2')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('def')");
  }

  public void testHeadingNewStyleWithAppendixStyle() {
    doTest("[appendix]\n= Abc\nText\n",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('appendix')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADING ('= Abc')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testHeadingOldStyleWithHeaderTwoLinesAndAttribute() {
    doTest("Abc\n===\nHeader1\n:attr: val\nHeader2\ndef",
      "AsciiDoc:HEADING_OLDSTYLE ('Abc\\n===')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attr')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')\n" +
        "AsciiDoc:ATTRIBUTE_VAL (' val')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:HEADER ('Header2')\n" +
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
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('Caption')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('abc')");
  }

  public void testInlineMacroUrl() {
    doTest("image:http://image.com[Caption]\nabc",
      "AsciiDoc:INLINE_MACRO_ID ('image:')\n" +
        "AsciiDoc:URL_LINK ('http://image.com')\n" +
        "AsciiDoc:INLINE_ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('Caption')\n" +
        "AsciiDoc:INLINE_ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('abc')");
  }

  public void testBlockMacroWithAttribute() {
    doTest("macro::foo[key=value]",
      "AsciiDoc:BLOCK_MACRO_ID ('macro::')\n" +
        "AsciiDoc:BLOCK_MACRO_BODY ('foo')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('key')\n" +
        "AsciiDoc:ASSIGNMENT ('=')\n" +
        "AsciiDoc:ATTR_VALUE ('value')\n" +
        "AsciiDoc:ATTRS_END (']')");
  }

  public void testBlockMacroWithSingleQuotedAttribute() {
    doTest("macro::foo[key='value']",
      "AsciiDoc:BLOCK_MACRO_ID ('macro::')\n" +
        "AsciiDoc:BLOCK_MACRO_BODY ('foo')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('key')\n" +
        "AsciiDoc:ASSIGNMENT ('=')\n" +
        "AsciiDoc:SINGLE_QUOTE (''')\n" +
        "AsciiDoc:ATTR_VALUE ('value')\n" +
        "AsciiDoc:SINGLE_QUOTE (''')\n" +
        "AsciiDoc:ATTRS_END (']')");
  }

  public void testBlockMacroWithDoubleQuotedAttribute() {
    doTest("macro::foo[key=\"value\"]",
      "AsciiDoc:BLOCK_MACRO_ID ('macro::')\n" +
        "AsciiDoc:BLOCK_MACRO_BODY ('foo')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('key')\n" +
        "AsciiDoc:ASSIGNMENT ('=')\n" +
        "AsciiDoc:DOUBLE_QUOTE ('\"')\n" +
        "AsciiDoc:ATTR_VALUE ('value')\n" +
        "AsciiDoc:DOUBLE_QUOTE ('\"')\n" +
        "AsciiDoc:ATTRS_END (']')");
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
      "AsciiDoc:TITLE_TOKEN ('.')\n" +
        "AsciiDoc:TEXT ('Foo')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('bar')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('baz')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Foo')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('bar')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('baz')");
  }

  public void testBlockAttrs() {
    doTest("[NOTE]\n",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('NOTE')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testNoBlockAttrs() {
    doTest("[nolink]:: Term",
      "AsciiDoc:LBRACKET ('[')\n" +
        "AsciiDoc:DESCRIPTION ('nolink')\n" +
        "AsciiDoc:RBRACKET (']')\n" +
        "AsciiDoc:DESCRIPTION ('::')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Term')");
  }

  public void testUnclosedBlockAttrs() {
    doTest("[\nfoo",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('foo')");
  }

  public void testOldStyleHeading() {
    doTest("Hi\n--\n",
      "AsciiDoc:HEADING_OLDSTYLE ('Hi\\n--')\n" +
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

  public void testAttributeEmptyAtEnd() {
    doTest(":attribute!:",
      "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute')\n" +
        "AsciiDoc:ATTRIBUTE_UNSET ('!')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')");
  }

  public void testAttributeWithBlanks() {
    doTest(":attri b ute :",
      "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attri b ute ')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')");
  }


  public void testAttributeEmptyAtStart() {
    doTest(":attribute!:",
      "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute')\n" +
        "AsciiDoc:ATTRIBUTE_UNSET ('!')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')");
  }

  public void testAttributeWithBracket() {
    doTest(":attr: icon:check[]",
      "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attr')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')\n" +
        "AsciiDoc:ATTRIBUTE_VAL (' icon:check[]')");
  }

  public void testAttributeInTitle() {
    doTest(".xx{hi}xx",
      "AsciiDoc:TITLE_TOKEN ('.')\n" +
        "AsciiDoc:TEXT ('xx')\n" +
        "AsciiDoc:ATTRIBUTE_REF_START ('{')\n" +
        "AsciiDoc:ATTRIBUTE_REF ('hi')\n" +
        "AsciiDoc:ATTRIBUTE_REF_END ('}')\n" +
        "AsciiDoc:TEXT ('xx')");
  }

  public void testBracketInBlockAttributes() {
    doTest("[val=\"{attr}[xx]\"]",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('val')\n" +
        "AsciiDoc:ASSIGNMENT ('=')\n" +
        "AsciiDoc:DOUBLE_QUOTE ('\"')\n" +
        "AsciiDoc:ATTRIBUTE_REF_START ('{')\n" +
        "AsciiDoc:ATTRIBUTE_REF ('attr')\n" +
        "AsciiDoc:ATTRIBUTE_REF_END ('}')\n" +
        "AsciiDoc:ATTR_VALUE ('[xx]')\n" +
        "AsciiDoc:DOUBLE_QUOTE ('\"')\n" +
        "AsciiDoc:ATTRS_END (']')");
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

  public void testAttributeWithNestedAttributeAndValue() {
    doTest(":attribute: {otherattr}value",
      "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')\n" +
        "AsciiDoc:ATTRIBUTE_VAL (' ')\n" +
        "AsciiDoc:ATTRIBUTE_REF_START ('{')\n" +
        "AsciiDoc:ATTRIBUTE_REF ('otherattr')\n" +
        "AsciiDoc:ATTRIBUTE_REF_END ('}')\n" +
        "AsciiDoc:ATTRIBUTE_VAL ('value')");
  }

  /**
   * Value continue on the next line if the line is ended by a space followed by a backslash.
   */
  public void testAttributeMultiline() {
    doTest(":attribute: value \\\n continue on the next line\nMore text",
      "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')\n" +
        "AsciiDoc:ATTRIBUTE_VAL (' value')\n" +
        "AsciiDoc:ATTRIBUTE_CONTINUATION (' \\\\n ')\n" +
        "AsciiDoc:ATTRIBUTE_VAL ('continue on the next line')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('More')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('text')");
  }

  /**
   * Value continue on the next line if the line is ended by a space followed by a backslash.
   */
  public void testAttributeMultilineWithPlus() {
    //noinspection AsciiDocAttributeContinuationInspection
    doTest(":attribute: value +\n continue on the next line\nMore text",
      "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')\n" +
        "AsciiDoc:ATTRIBUTE_VAL (' value')\n" +
        "AsciiDoc:ATTRIBUTE_CONTINUATION_LEGACY (' +\\n ')\n" +
        "AsciiDoc:ATTRIBUTE_VAL ('continue on the next line')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('More')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('text')");
  }

  public void testTwoConsecutiveAttributes() {
    doTest("Text\n\n:attribute1:\n:attribute2:",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute1')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('attribute2')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')");
  }

  public void testNoAttributeAfterText() {
    doTest("Text\n:attribute1:\n",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT (':attribute1:')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
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

  public void testContinuationThenTitle() {
    doTest("+\n.Title",
      "AsciiDoc:CONTINUATION ('+')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TITLE_TOKEN ('.')\n" +
        "AsciiDoc:TEXT ('Title')");
  }

  public void testAnchorThenTitle() {
    doTest("[#anchor]\n.Title",
      "AsciiDoc:BLOCKIDSTART ('[#')\n" +
        "AsciiDoc:BLOCKID ('anchor')\n" +
        "AsciiDoc:BLOCKIDEND (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TITLE_TOKEN ('.')\n" +
        "AsciiDoc:TEXT ('Title')");
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
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
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

  public void testConstrainedNumber() {
    doTest("11_11_11",
      "AsciiDoc:TEXT ('11_11_11')");
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
        "AsciiDoc:REF ('FILE#REF')\n" +
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

  public void testEscapedLinkText() {
    doTest("link:FILE[T\\]ext]",
      "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:LINKFILE ('FILE')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKTEXT ('T\\]ext')\n" +
        "AsciiDoc:LINKEND (']')");
  }

  public void testAttrInUrl() {
    doTest("http://url.com{path}[text]",
      "AsciiDoc:URL_LINK ('http://url.com')\n" +
        "AsciiDoc:ATTRIBUTE_REF_START ('{')\n" +
        "AsciiDoc:ATTRIBUTE_REF ('path')\n" +
        "AsciiDoc:ATTRIBUTE_REF_END ('}')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKTEXT ('text')\n" +
        "AsciiDoc:LINKEND (']')");
  }

  public void testHtmlEntity() {
    doTest("Hi&amp;Ho",
      "AsciiDoc:TEXT ('Hi')\n" +
        "AsciiDoc:HTML_ENTITY ('&amp;')\n" +
        "AsciiDoc:TEXT ('Ho')");
  }

  public void testUnicodeDecimalEntity() {
    doTest("Hi&#123;Ho",
      "AsciiDoc:TEXT ('Hi')\n" +
        "AsciiDoc:HTML_ENTITY ('&#123;')\n" +
        "AsciiDoc:TEXT ('Ho')");
  }

  public void testUnicodeHexEntity() {
    doTest("Hi&#x123Af;Ho",
      "AsciiDoc:TEXT ('Hi')\n" +
        "AsciiDoc:HTML_ENTITY ('&#x123Af;')\n" +
        "AsciiDoc:TEXT ('Ho')");
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
      "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:URL_LINK ('http://url.com')\n" +
        "AsciiDoc:ATTRIBUTE_REF_START ('{')\n" +
        "AsciiDoc:ATTRIBUTE_REF ('path')\n" +
        "AsciiDoc:ATTRIBUTE_REF_END ('}')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKTEXT ('text')\n" +
        "AsciiDoc:LINKEND (']')");
  }

  public void testLinkWithAttributeAutocomplete() {
    doTest("link:IntellijIdeaRulezzz test.adoc[]\n",
      "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:LINKFILE ('IntellijIdeaRulezzz test.adoc')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKEND (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testLinkInFormatting() {
    doTest("`http://localhost:8080/`",
      "AsciiDoc:MONO_START ('`')\n" +
        "AsciiDoc:URL_LINK ('http://localhost:8080/')\n" +
        "AsciiDoc:MONO_END ('`')");

  }

  public void testLinkCharLikeFormattingAtEnd() {
    doTest("http://localhost:8080/`",
      "AsciiDoc:URL_LINK ('http://localhost:8080/`')\n");
  }

  public void testLinkWithTitleAndContinuation() {
    doTest("link:test.adoc[Title +\nContinuing]\n",
      "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:LINKFILE ('test.adoc')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKTEXT ('Title')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:CONTINUATION ('+')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LINKTEXT ('Continuing')\n" +
        "AsciiDoc:LINKEND (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
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

  public void testLinkWithQuotes() {
    doTest("Text link:++https://example.org/?q=[a b]++[URL with special characters] Text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:URL_LINK ('++https://example.org/?q=[a b]++')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKTEXT ('URL')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:LINKTEXT ('with')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:LINKTEXT ('special')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:LINKTEXT ('characters')\n" +
        "AsciiDoc:LINKEND (']')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testLinkForAutocomplete() {
    doTest("Text link:FILEIntellijIdeaRulezzz More Text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:LINKFILE ('FILEIntellijIdeaRulezzz More')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testLinkForAutocompleteWithBrackets() {
    doTest("link:IntellijIdeaRulezzz []",
      "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:LINKFILE ('IntellijIdeaRulezzz ')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKEND (']')");
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

  public void testMultipleDoubleTypographicQuotes() {
    doTest("\"`test?`\" \"`test?`\"",
      "AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('\"`')\n" +
        "AsciiDoc:TEXT ('test?')\n" +
        "AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`\"')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('\"`')\n" +
        "AsciiDoc:TEXT ('test?')\n" +
        "AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`\"')");
  }

  public void testMultiplSingleTypographicQuotes() {
    doTest("'`test?`' '`test?`'",
      "AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_START (''`')\n" +
        "AsciiDoc:TEXT ('test?')\n" +
        "AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_END ('`'')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TYPOGRAPHIC_SINGLE_QUOTE_START (''`')\n" +
        "AsciiDoc:TEXT ('test?')\n" +
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

  public void testTwoTypographicQuotesThatMightBeConsideredAMonospace() {
    doTest("\"`Test?`\", and \"`What?`\"",
      "AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('\"`')\n" +
        "AsciiDoc:TEXT ('Test?')\n" +
        "AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`\"')\n" +
        "AsciiDoc:TEXT (',')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('and')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_START ('\"`')\n" +
        "AsciiDoc:TEXT ('What?')\n" +
        "AsciiDoc:TYPOGRAPHIC_DOUBLE_QUOTE_END ('`\"')");
  }

  public void testNoTypographicQuotesNonMatching() {
    doTest("\"`test",
      "AsciiDoc:DOUBLE_QUOTE ('\"')\n" +
        "AsciiDoc:TEXT ('`test')");
  }

  public void testPassThroughInlineThreePlus() {
    doTest("+++pt\npt2+++",
      "AsciiDoc:PASSTRHOUGH_INLINE_START ('+++')\n" +
        "AsciiDoc:PASSTRHOUGH_CONTENT ('pt\\npt2')\n" +
        "AsciiDoc:PASSTRHOUGH_INLINE_END ('+++')");
  }

  public void testPassThroughInlineOnePlus() {
    doTest("+pt\np+t2+",
      "AsciiDoc:PASSTRHOUGH_INLINE_START ('+')\n" +
        "AsciiDoc:PASSTRHOUGH_CONTENT ('pt\\np+t2')\n" +
        "AsciiDoc:PASSTRHOUGH_INLINE_END ('+')");
  }

  public void testPassThroughInlineTwoPlus() {
    doTest("++pt\npt2++",
      "AsciiDoc:PASSTRHOUGH_INLINE_START ('++')\n" +
        "AsciiDoc:PASSTRHOUGH_CONTENT ('pt\\npt2')\n" +
        "AsciiDoc:PASSTRHOUGH_INLINE_END ('++')");
  }

  public void testPassThroughDoublePlusAndSingle() {
    doTest("++text++and +some+ other",
      "AsciiDoc:PASSTRHOUGH_INLINE_START ('++')\n" +
        "AsciiDoc:PASSTRHOUGH_CONTENT ('text')\n" +
        "AsciiDoc:PASSTRHOUGH_INLINE_END ('++')\n" +
        "AsciiDoc:TEXT ('and')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('+some+')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('other')");
  }

  public void testPassThroughRunOff() {
    doTest("+pt+test\n\nHi",
      "AsciiDoc:PASSTRHOUGH_INLINE_START ('+')\n" +
        "AsciiDoc:PASSTRHOUGH_CONTENT ('pt+test\\n')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Hi')");
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

  public void testListingNestedInExample() {
    doTest("====\n----\n----\n====\n",
      "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testListingWithCallout() {
    doTest("----\n----\n<1> Callout 1\n<.> Callout 2",
      "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:CALLOUT ('<1>')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Callout')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:CALLOUT ('<.>')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Callout')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('2')");
  }

  public void testTitleAfterId() {
    doTest("[[id]]\n.Title\n====\nExample\n====",
      "AsciiDoc:BLOCKIDSTART ('[[')\n" +
        "AsciiDoc:BLOCKID ('id')\n" +
        "AsciiDoc:BLOCKIDEND (']]')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TITLE_TOKEN ('.')\n" +
        "AsciiDoc:TEXT ('Title')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Example')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('====')");
  }

  public void testTitleSTartingWithADot() {
    doTest("..gitignore\n----\nExample\n----",
      "AsciiDoc:TITLE_TOKEN ('.')\n" +
        "AsciiDoc:TEXT ('.gitignore')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('Example')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')");
  }

  public void testDoubleColonNotEndOfSentence() {
    doTest("::\n",
      "AsciiDoc:TEXT ('::')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testInitialNotEndOfSentenceMiddleOfLine() {
    doTest("Wolfgang A. Mozart",
      "AsciiDoc:TEXT ('Wolfgang')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('A.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Mozart')");
  }

  public void testHardBreakWithContinuation() {
    doTest("* Test +\n+\nsecond line",
      "AsciiDoc:BULLET ('*')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Test')\n" +
        "AsciiDoc:HARD_BREAK (' +')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:CONTINUATION ('+')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('second')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('line')");
  }

  public void testHardBreakAtBlockEnd() {
    doTest("|===\n" +
      "|XX +\n" +
      "|===\n" +
      "\n" +
      "== Title",
      "AsciiDoc:BLOCK_DELIMITER ('|===')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('|XX')\n" +
        "AsciiDoc:HARD_BREAK (' +')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('|===')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:HEADING ('== Title')");
  }

  public void testInitialEndOfSentenceAtEndOfLineSoThatItKeepsExistingWraps() {
    doTest("Wolfgang A.\nMozart",
      "AsciiDoc:TEXT ('Wolfgang')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('A')\n" +
        "AsciiDoc:END_OF_SENTENCE ('.')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Mozart')");
  }

  public void testDonWrapIfFollowedByNumberInsideLine() {
    doTest("Ch. 9 important",
      "AsciiDoc:TEXT ('Ch.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('9')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('important')");
  }

  public void testDonWrapIfFollowedByNumberNextLineLine() {
    doTest("Ch.\n9 important",
      "AsciiDoc:TEXT ('Ch')\n" +
        "AsciiDoc:END_OF_SENTENCE ('.')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('9')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('important')");
  }

  public void testInitialEndOfSentenceAtEndOfLineSoThatItKeepsExistingWrapsEvenIfThereIsABlankAtTheEndOfTheLine() {
    doTest("Wolfgang A. \nMozart",
      "AsciiDoc:TEXT ('Wolfgang')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('A')\n" +
        "AsciiDoc:END_OF_SENTENCE ('.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Mozart')");
  }

  public void testExampleWithBlankLine() {
    doTest("====\nTest\n\n====\n",
      "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Test')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testExampleWithListing() {
    doTest("====\n.Title\n[source]\n----\nSource\n----\n====\n",
      "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TITLE_TOKEN ('.')\n" +
        "AsciiDoc:TEXT ('Title')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('source')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('Source')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
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

  public void testEndOfSentenceWithUmlaut() {
    doTest("End. Öf Sentence",
      "AsciiDoc:TEXT ('End')\n" +
        "AsciiDoc:END_OF_SENTENCE ('.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Öf')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Sentence')");
  }

  public void testNoEndOfSentence() {
    doTest("End.No Sentence",
      "AsciiDoc:TEXT ('End.No')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Sentence')");
  }

  public void testNoEndOfSentenceAfterNumber() {
    doTest("After 1. Number",
      "AsciiDoc:TEXT ('After')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('1.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Number')");
  }

  public void testNoEndOfSentenceAfterColon() {
    doTest("Colon: Word",
      "AsciiDoc:TEXT ('Colon:')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Word')");
  }

  public void testEndOfSentenceAfterColonAndNewline() {
    doTest("Colon:\nWord",
      "AsciiDoc:TEXT ('Colon')\n" +
        "AsciiDoc:END_OF_SENTENCE (':')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Word')");
  }

  public void testNoEndOfSentenceAgain() {
    doTest("End. no Sentence",
      "AsciiDoc:TEXT ('End.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('no')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Sentence')");
  }

  public void testNoEndOfSentenceAdExemplar() {
    doTest("e.g. No Sentence",
      "AsciiDoc:TEXT ('e.g.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('No')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Sentence')");
  }

  public void testDescription() {
    doTest("a property:: description",
      "AsciiDoc:DESCRIPTION ('a')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:DESCRIPTION ('property::')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('description')");
  }

  public void testDescriptionWithFormatting() {
    doTest("`property`:: description",
      "AsciiDoc:MONO_START ('`')\n" +
        "AsciiDoc:DESCRIPTION ('property')\n" +
        "AsciiDoc:MONO_END ('`')\n" +
        "AsciiDoc:DESCRIPTION ('::')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('description')");
  }

  public void testDescriptionWithLink() {
    doTest("link:http://www.example.com[Example]:: description",
      "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:URL_LINK ('http://www.example.com')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKTEXT ('Example')\n" +
        "AsciiDoc:LINKEND (']')\n" +
        "AsciiDoc:DESCRIPTION ('::')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('description')");
  }

  public void testDescriptionWithAttribute() {
    doTest("{attr}:: description",
      "AsciiDoc:ATTRIBUTE_REF_START ('{')\n" +
        "AsciiDoc:ATTRIBUTE_REF ('attr')\n" +
        "AsciiDoc:ATTRIBUTE_REF_END ('}')\n" +
        "AsciiDoc:DESCRIPTION ('::')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('description')");
  }

  public void testIndentedListing() {
    doTest("   Listing\nMore\n\nText",
      "AsciiDoc:LISTING_TEXT ('   Listing')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('More')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testListingWithNoDelimiters() {
    doTest("[source]\nListing\n\nText",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('source')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('Listing')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testListingWithLanguage() {
    doTest("[source,php]\nListing\n\nText",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('source')\n" +
        "AsciiDoc:SEPARATOR (',')\n" +
        "AsciiDoc:ATTR_NAME ('php')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('Listing')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testListingWithOpenBlock() {
    doTest("[source]\n--\nListing\n--\nText",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('source')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('Listing')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('--')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testPassthroughWithNoDelimiters() {
    doTest("[pass]\nPas**ss**ss\n\nText",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('pass')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:PASSTRHOUGH_CONTENT ('Pas**ss**ss')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testListingWithAttributeAndDelimiter() {
    doTest("[source]\n----\nListing\n----\nText",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('source')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('Listing')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testVerseWithCommentNoDelimiters() {
    doTest("[verse]\n" +
        "// test\n" +
        " Verse\n",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('verse')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LINE_COMMENT ('// test')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Verse')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testBlockWithTitleInsideExample() {
    doTest("====\n" +
        "Text\n" +
        "\n" +
        ".Title\n" +
        "----\n" +
        "Hi\n" +
        "----\n" +
        "====",
      "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:TITLE_TOKEN ('.')\n" +
        "AsciiDoc:TEXT ('Title')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('Hi')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_BLOCK_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('====')");
  }

  public void testVerseWithSomethingLookingLikeBlock() {
    doTest("[verse]\n" +
        "V1\n" +
        "----\n" +
        "V2\n" +
        "\n" +
        "[source]\n" +
        "Hi",
      "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('verse')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('V1')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('V2')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('source')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('Hi')");
  }

  public void testEnumeration() {
    doTest(". Item",
      "AsciiDoc:ENUMERATION ('.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Item')");
  }

  public void testEnumerationNumber() {
    doTest("1. Item",
      "AsciiDoc:ENUMERATION ('1.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Item')");
  }

  public void testEnumerationCharacter() {
    doTest("a. Item",
      "AsciiDoc:ENUMERATION ('a.')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Item')");
  }

  public void testEndingBlockWithNoDelimiterInsideBlockWithDelimiter() {
    doTest("====\n" +
        "[verse]\n" +
        "test\n" +
        "----\n" +
        "====\n",
      "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('verse')\n" +
        "AsciiDoc:ATTRS_END (']')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('test')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testInlineMacro() {
    doTest("Text image:image.png[] text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:INLINE_MACRO_ID ('image:')\n" +
        "AsciiDoc:INLINE_MACRO_BODY ('image.png')\n" +
        "AsciiDoc:INLINE_ATTRS_START ('[')\n" +
        "AsciiDoc:INLINE_ATTRS_END (']')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('text')");
  }

  public void testInlineMacroMultiLine() {
    doTest("image:image.png[Text\nText]",
      "AsciiDoc:INLINE_MACRO_ID ('image:')\n" +
        "AsciiDoc:INLINE_MACRO_BODY ('image.png')\n" +
        "AsciiDoc:INLINE_ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('Text')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:ATTR_NAME ('Text')\n" +
        "AsciiDoc:INLINE_ATTRS_END (']')");
  }

  public void testInlineMacroWithAttribute() {
    doTest("Text image:image.png[link=http://www.gmx.net] text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:INLINE_MACRO_ID ('image:')\n" +
        "AsciiDoc:INLINE_MACRO_BODY ('image.png')\n" +
        "AsciiDoc:INLINE_ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('link')\n" +
        "AsciiDoc:ASSIGNMENT ('=')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:INLINE_ATTRS_END (']')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('text')");
  }

  public void testInlineMacroWithAttributeRef() {
    doTest("Text image:image.png[link={url}] text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:INLINE_MACRO_ID ('image:')\n" +
        "AsciiDoc:INLINE_MACRO_BODY ('image.png')\n" +
        "AsciiDoc:INLINE_ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('link')\n" +
        "AsciiDoc:ASSIGNMENT ('=')\n" +
        "AsciiDoc:ATTRIBUTE_REF_START ('{')\n" +
        "AsciiDoc:ATTRIBUTE_REF ('url')\n" +
        "AsciiDoc:ATTRIBUTE_REF_END ('}')\n" +
        "AsciiDoc:INLINE_ATTRS_END (']')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('text')");
  }

  public void testInlineMacroWithBracketsInside() {
    doTest("Text footnote:[some macro:text[About]] text",
      "AsciiDoc:TEXT ('Text')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:INLINE_MACRO_ID ('footnote:')\n" +
        "AsciiDoc:INLINE_ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('some')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:ATTR_NAME ('macro:text[About]')\n" +
        "AsciiDoc:INLINE_ATTRS_END (']')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('text')");
  }

  public void testBlockMacroWithBracketsInside() {
    doTest("macro::text[other:[hi]]",
      "AsciiDoc:BLOCK_MACRO_ID ('macro::')\n" +
        "AsciiDoc:BLOCK_MACRO_BODY ('text')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('other:[hi]')\n" +
        "AsciiDoc:ATTRS_END (']')");
  }

  public void testExampleWithListingNoDelimiter() {
    doTest("====\n" +
        " Test\n" +
        "====\n",
      "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT (' Test')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:BLOCK_DELIMITER ('====')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')");
  }

  public void testEllipseInsideLIne() {
    doTest("Text... Text",
      "AsciiDoc:TEXT ('Text...')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testResetFormatting() {
    doTest("`Mono`Text\n\nText",
      "AsciiDoc:MONO_START ('`')\n" +
        "AsciiDoc:MONO ('Mono`Text')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:EMPTY_LINE ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testIfDef() {
    doTest("ifdef::attr[]",
      "AsciiDoc:BLOCK_MACRO_ID ('ifdef::')\n" +
        "AsciiDoc:ATTRIBUTE_REF ('attr')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTRS_END (']')");
  }

  public void testIfDefWithAttributeInBody() {
    doTest("ifdef::attr[:other: val]",
      "AsciiDoc:BLOCK_MACRO_ID ('ifdef::')\n" +
        "AsciiDoc:ATTRIBUTE_REF ('attr')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_START (':')\n" +
        "AsciiDoc:ATTRIBUTE_NAME ('other')\n" +
        "AsciiDoc:ATTRIBUTE_NAME_END (':')\n" +
        "AsciiDoc:ATTRIBUTE_VAL (' val')\n" +
        "AsciiDoc:ATTRS_END (']')");
  }

  public void testLinkAttribute() {
    doTest("image::file.png[link='http://www.gmx.net']",
      "AsciiDoc:BLOCK_MACRO_ID ('image::')\n" +
        "AsciiDoc:BLOCK_MACRO_BODY ('file.png')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTR_NAME ('link')\n" +
        "AsciiDoc:ASSIGNMENT ('=')\n" +
        "AsciiDoc:SINGLE_QUOTE (''')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:SINGLE_QUOTE (''')\n" +
        "AsciiDoc:ATTRS_END (']')");
  }

  public void testEndifSpecial() {
    doTest("endif::[]",
      "AsciiDoc:BLOCK_MACRO_ID ('endif::')\n" +
        "AsciiDoc:ATTRS_START ('[')\n" +
        "AsciiDoc:ATTRS_END (']')");
  }

  public void testSimpleUrl() {
    doTest("http://www.gmx.net",
      "AsciiDoc:URL_LINK ('http://www.gmx.net')");
  }

  public void testSimpleUrlAtEndOfSentence() {
    doTest("http://www.gmx.net.",
      "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:TEXT ('.')");
  }

  public void testSimpleUrlInParentheses() {
    doTest("(http://www.gmx.net)",
      "AsciiDoc:LPAREN ('(')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:RPAREN (')')");
  }

  public void testSimpleUrlInParenthesesWithColon() {
    doTest("(http://www.gmx.net):",
      "AsciiDoc:LPAREN ('(')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:RPAREN (')')\n" +
        "AsciiDoc:TEXT (':')");
  }

  public void testSimpleUrlInParenthesesAndText() {
    doTest("(http://www.gmx.net) Text",
      "AsciiDoc:LPAREN ('(')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:RPAREN (')')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testSimpleUrlInParenthesesWithColonAndText() {
    doTest("(http://www.gmx.net): Text",
      "AsciiDoc:LPAREN ('(')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:RPAREN (')')\n" +
        "AsciiDoc:TEXT (':')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testSimpleUrlInParenthesesAtEndOfLine() {
    doTest("(http://www.gmx.net)\nText",
      "AsciiDoc:LPAREN ('(')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:RPAREN (')')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testSimpleUrlInParenthesesWithColonAtEndOfLine() {
    doTest("(http://www.gmx.net):\nText",
      "AsciiDoc:LPAREN ('(')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:RPAREN (')')\n" +
        "AsciiDoc:END_OF_SENTENCE (':')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('Text')");
  }

  public void testUrlInBrackets() {
    doTest("<http://www.gmx.net>",
      "AsciiDoc:URL_START ('<')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:URL_END ('>')");
  }

  public void testUrlInBracketsWithSpace() {
    doTest("<http://www.gmx.net >",
      "AsciiDoc:LT ('<')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:WHITE_SPACE (' ')\n" +
        "AsciiDoc:GT ('>')");
  }

  public void testUrlInBracketsWithSquareBracket() {
    doTest("<http://www.gmx.net[Hi]>",
      "AsciiDoc:LT ('<')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKTEXT ('Hi')\n" +
        "AsciiDoc:LINKEND (']')\n" +
        "AsciiDoc:GT ('>')");
  }

  public void testUrlWithLinkPrefix() {
    doTest("link:http://www.gmx.net[Hi]",
      "AsciiDoc:LINKSTART ('link:')\n" +
        "AsciiDoc:URL_LINK ('http://www.gmx.net')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKTEXT ('Hi')\n" +
        "AsciiDoc:LINKEND (']')");
  }

  public void testEmail() {
    doTest("doc.writer@example.com",
      "AsciiDoc:URL_EMAIL ('doc.writer@example.com')");
  }

  public void testEmailWithPrefix() {
    doTest("mailto:doc.writer@example.com[]",
      "AsciiDoc:URL_PREFIX ('mailto:')\n" +
        "AsciiDoc:URL_EMAIL ('doc.writer@example.com')\n" +
        "AsciiDoc:LINKTEXT_START ('[')\n" +
        "AsciiDoc:LINKEND (']')");
  }

  public void testEmailWithPrefixButNoSquareBrackets() {
    doTest("mailto:doc.writer@example.com",
      "AsciiDoc:TEXT ('mailto:doc.writer@example.com')");
  }

  public void testFrontmatter() {
    doTest("---\nhi: ho\n---",
      "AsciiDoc:FRONTMATTER_DELIMITER ('---')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:FRONTMATTER ('hi: ho')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:FRONTMATTER_DELIMITER ('---')");
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

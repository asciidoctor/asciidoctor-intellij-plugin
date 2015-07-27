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
    doTest("aaa\n----\nbbbb\n----\ncccc",
        "AsciiDoc:TEXT ('aaa')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_TEXT ('bbbb')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:LISTING_DELIMITER ('----')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
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

  public void testCommentBlock() {
    doTest("////\nfoo bar\n////\nabc",
        "AsciiDoc:BLOCK_COMMENT ('////\\nfoo bar\\n////')\n" +
        "AsciiDoc:LINE_BREAK ('\\n')\n" +
        "AsciiDoc:TEXT ('abc')");
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

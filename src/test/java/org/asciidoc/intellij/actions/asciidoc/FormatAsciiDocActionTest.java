package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.mock.MockDocument;
import com.intellij.openapi.editor.Document;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Alexander Schwartz 2016
 */
@RunWith(Parameterized.class)
public class FormatAsciiDocActionTest {

  @Parameterized.Parameters(name = "{index}: isWord(\"{0}{1}{2}\") == {3} ({4})")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {" ", "test", " ", true, "word surrounded by blanks"},
      {"", "test", " ", true, "word prefixed with nothing"},
      {" ", "test", "", true, "word postfixed with nothing"},
      {"_", "test", " ", false, "word prefixed with underscore"},
      {" ", "test", "_", false, "word postfixed with underscore"},
      {":", "test", " ", false, "word prefixed with colon"},
      {";", "test", " ", false, "word prefixed with semicolon"},
      {" ", "123", " ", true, "numbers surrounded by blanks"},
      {"a", "123", " ", false, "numbers prefixed by letter"},
      {" ", "123", "b", false, "numbers postfixed by letter"},
      {" ", "\u00E4rger", " ", true, "word with umlaut surrounded by blanks"},
      {" ", "regr\u00E4", " ", true, "word with umlaut surrounded by blanks"},
      {"\u00E4", "123", " ", false, "numbers prefixed by umlaut"},
      {" ", "123", "\u00E4", false, "numbers postfixed by umlaut"},
      {" ", " test", " ", false, "word with surrounding space also selected"},
      {" ", "test", "'s", false, "word with apostrophe should avoid typographic quote"},
      {"'", "test", "'", true, "word with single quotes should lead to typographic quote"},
      {"\"", "test", "\"", true, "word with single quotes should lead to typographic quote"},
      {"", "", "", true, "empty string with no prefix and suffix"},
    });
  }

  private final Document document;
  private final int start;
  private final int end;
  private final String explanation;
  private final boolean isWord;

  public FormatAsciiDocActionTest(String prefix, String selection, String postfix, boolean isWord, String explanation) {
    document = new MockDocument();
    document.replaceString(0, 0, prefix + selection + postfix);
    start = prefix.length();
    end = prefix.length() + selection.length();
    this.explanation = explanation;
    this.isWord = isWord;
  }

  @Test
  public void shouldMarkWord() {
    Assert.assertEquals(explanation, isWord, FormatAsciiDocAction.isWord(document, start, end, "`"));
  }
}

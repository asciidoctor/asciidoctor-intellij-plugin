package org.asciidoc.intellij.editor;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.assertj.core.api.Assertions;
import org.intellij.lang.annotations.Language;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExtendWordSelectionHandlerTest extends BasePlatformTestCase {

  public static final String CARET = "<caret>";

  public void testCursorAtSingleQuote() {
    // given...
    List<String> ranges = prepareRanges("Some <caret>\" Text");

    // then...
    Assertions.assertThat(ranges).containsExactlyInAnyOrder("\"");
  }

  public void testCursorWithinQuotes() {
    // given...
    List<String> ranges = prepareRanges("This is a `<caret>word` in a sentence.");

    // then...
    Assertions.assertThat(ranges).containsExactlyInAnyOrder("word", "`word`");
  }

  @SuppressWarnings("AsciiDocLinkResolve")
  public void testCursorWithinBlockMacro() {
    // given...
    List<String> ranges = prepareRanges("This image:file.png[alt='<caret>text'] inside the line.");

    // then...
    Assertions.assertThat(ranges).containsExactlyInAnyOrder("text", "'text'", "alt='text'", "[alt='text']", "image:file.png[alt='text']");
  }

  public void testCursorWithSentences() {
    // given...
    List<String> ranges = prepareRanges("One sentence. (Another <caret>sentence) word. Third sentence.");

    // then...
    Assertions.assertThat(ranges).containsExactlyInAnyOrder("sentence", "Another sentence", "(Another sentence)", "(Another sentence) word.");
  }

  private List<String> prepareRanges(@Language("asciidoc") String content) {
    // parse caret from text string so we don't need to pass it as a parameter
    int cursor = content.indexOf(CARET);
    Assertions.assertThat(cursor).withFailMessage("no caret marker found").isNotEqualTo(-1);
    String contentWithoutCaret = content.replaceAll(CARET, "");

    // use content to setup editor environment
    PsiFile psiFile = configureByAsciiDoc(content);
    PsiElement element = psiFile.findElementAt(cursor);
    Objects.requireNonNull(element);

    // compute ranges from content
    ExtendWordSelectionHandler selectionHandler = new ExtendWordSelectionHandler();
    Assertions.assertThat(selectionHandler.canSelect(psiFile)).isTrue();
    List<TextRange> ranges = selectionHandler.select(element, contentWithoutCaret, cursor, myFixture.getEditor());
    if (ranges == null) {
      return Collections.emptyList();
    }
    return ranges.stream()
      // translate text ranges to the snippet in the content
      .map(textRange -> textRange.substring(contentWithoutCaret))
      .distinct()
      .sorted()
      .filter(s -> !s.equals(contentWithoutCaret)) // remove full match for simplicity
      .collect(Collectors.toList());
  }

  private PsiFile configureByAsciiDoc(@Language("asciidoc") String text) {
    return myFixture.configureByText(AsciiDocFileType.INSTANCE, text);
  }

}

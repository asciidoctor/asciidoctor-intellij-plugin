package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PasteTableActionTest {

  @Test
  public void shouldKeepCellWithBlankLinesTogether() {
    String data = "A\t\"line1\n\nline2\"\nB\tC\n";
    assertThat(ClipboardTableParser.parse(data, "\t"))
      .isEqualTo(List.of(List.of("A", "line1\n\nline2"), List.of("B", "C")));
    assertThat(PasteTableAction.toAsciiDocTable(data, "\t", false))
      .isEqualTo("\n|===\n|Header 1 |Header 2\n\n|A\n|line1\n\nline2\n\n|B\n|C\n\n|===\n");
  }

  @Test
  public void shouldUnescapeQuotesAndHandleCrLf() {
    assertThat(ClipboardTableParser.parse("\"say \"\"hi\"\", ok\",x\r\ny,z\r\n", ","))
      .isEqualTo(List.of(List.of("say \"hi\", ok", "x"), List.of("y", "z")));
  }

  @Test
  public void shouldNotTreatQuoteInsideOrUnclosedAsQuoting() {
    assertThat(ClipboardTableParser.parse("\"a\" b\tc\n\"open\td\n", "\t"))
      .isEqualTo(List.of(List.of("\"a\" b", "c"), List.of("\"open", "d")));
    assertThat(ClipboardTableParser.parse("a\"b\tc", "\t"))
      .isEqualTo(List.of(List.of("a\"b", "c")));
  }

  @Test
  public void shouldKeepEmptyCellsAndEscapePipes() {
    assertThat(ClipboardTableParser.parse("a\t\t\n\tb", "\t"))
      .isEqualTo(List.of(List.of("a", "", ""), List.of("", "b")));
    assertThat(PasteTableAction.toAsciiDocTable("a|b\tc", "\t", true))
      .isEqualTo("\n|===\n|a\\|b|c\n\n|===\n");
  }

  @Test
  public void shouldKeepHeaderOnOneLine() {
    assertThat(PasteTableAction.toAsciiDocTable("\"h\n1\"\th2\nx\ty\n", "\t", true))
      .isEqualTo("\n|===\n|h 1|h2\n\n|x\n|y\n\n|===\n");
  }
}

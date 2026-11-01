package org.asciidoc.intellij.actions.asciidoc;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PasteImageSelectionTest {

  @Test
  public void shouldCreateFileNameFromSelection() {
    assertThat(PasteImageSelection.toFileName(" Login Page / error 1 ", "png")).contains("login-page-error-1.png");
    assertThat(PasteImageSelection.toFileName("Überprüfung: Größe", "jpg")).contains("uberprufung-grosse.jpg");
  }

  @Test
  public void shouldNotDuplicateExtension() {
    assertThat(PasteImageSelection.toFileName("screenshot.PNG", "png")).contains("screenshot.png");
    assertThat(PasteImageSelection.toFileName("screenshot.png", "jpg")).contains("screenshot.png.jpg");
  }

  @Test
  public void shouldLimitLength() {
    assertThat(PasteImageSelection.toFileName("a".repeat(300), "png").orElseThrow()).hasSize(104);
  }

  @Test
  public void shouldIgnoreUnusableSelection() {
    assertThat(PasteImageSelection.toFileName(null, "png")).isEmpty();
    assertThat(PasteImageSelection.toFileName("   ", "png")).isEmpty();
    assertThat(PasteImageSelection.toFileName("///", "png")).isEmpty();
    assertThat(PasteImageSelection.toFileName("two\nlines", "png")).isEmpty();
    assertThat(PasteImageSelection.toAltText("two\nlines")).isEmpty();
  }

  @Test
  public void shouldKeepAltTextAsIs() {
    assertThat(PasteImageSelection.toAltText("  The login page / error  ")).contains("The login page / error");
  }
}

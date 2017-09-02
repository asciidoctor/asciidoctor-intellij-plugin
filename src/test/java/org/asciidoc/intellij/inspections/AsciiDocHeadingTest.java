package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocConvertMarkdownHeading;
import org.asciidoc.intellij.quickfix.AsciiDocConvertOldstyleHeading;

public class AsciiDocHeadingTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(AsciidocHeadingStyleInspection.class);
  }

  public void testOldStyleHeading() {
    doTest(AsciiDocConvertOldstyleHeading.NAME, true);
  }

  public void testMarkdownHeading() {
    doTest(AsciiDocConvertMarkdownHeading.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/heading";
  }
}

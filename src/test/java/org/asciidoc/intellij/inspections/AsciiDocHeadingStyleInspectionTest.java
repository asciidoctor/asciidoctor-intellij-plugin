package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocConvertMarkdownHeading;
import org.asciidoc.intellij.quickfix.AsciiDocConvertOldstyleHeading;

public class AsciiDocHeadingStyleInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // noinspection unchecked
    myFixture.enableInspections(AsciiDocHeadingStyleInspection.class);
  }

  public void testOldStyleHeading() {
    doTest(new AsciiDocConvertOldstyleHeading().getName(), true);
  }

  public void testMarkdownHeading() {
    doTest(new AsciiDocConvertMarkdownHeading().getName(), true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/heading";
  }
}

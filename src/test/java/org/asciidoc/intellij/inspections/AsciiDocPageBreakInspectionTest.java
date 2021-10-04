package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocShortenPagebreak;

public class AsciiDocPageBreakInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocShortenPagebreak().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // noinspection unchecked
    myFixture.enableInspections(AsciiDocPageBreakInspection.class);
  }

  public void testTooLongPagebreak() {
    doTest(NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/pagebreak";
  }
}

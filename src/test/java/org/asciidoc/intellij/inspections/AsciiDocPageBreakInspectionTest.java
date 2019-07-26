package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocShortenPagebreak;

public class AsciiDocPageBreakInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocPageBreakInspection.class);
  }

  public void testTooLongPagebreak() {
    doTest(AsciiDocShortenPagebreak.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/pagebreak";
  }
}

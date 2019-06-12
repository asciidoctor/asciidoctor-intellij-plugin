package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocShortenPagebreak;

public class AsciiDocPagebreakTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
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

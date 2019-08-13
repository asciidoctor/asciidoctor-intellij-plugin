package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocConvertAttributeContinuationLegacy;

public class AsciiDocAttributeContinuationInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocAttributeContinuationInspection.class);
  }

  public void testAttributeContinuation() {
    doTest(AsciiDocConvertAttributeContinuationLegacy.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/attributeContinuation";
  }
}

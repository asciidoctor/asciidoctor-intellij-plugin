package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocConvertAttributeContinuationLegacy;

public class AsciiDocAttributeContinuationInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocConvertAttributeContinuationLegacy().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocAttributeContinuationInspection.class);
  }

  public void testAttributeContinuation() {
    doTest(NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/attributeContinuation";
  }
}

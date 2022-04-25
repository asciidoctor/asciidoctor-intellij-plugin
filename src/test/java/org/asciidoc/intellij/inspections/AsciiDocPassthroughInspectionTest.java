package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocConvertPassthrough;

public class AsciiDocPassthroughInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocConvertPassthrough().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocPassthroughInspection.class);
  }

  public void testPassthroughV1() {
    doTest(NAME, true);
  }
  public void testPassthroughV2() {
    doTest(NAME, true);
  }
  public void testPassthroughV3() {
    doTest(NAME, true);
  }
  public void testPassthroughV4() {
    doTest(NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/passthrough";
  }
}

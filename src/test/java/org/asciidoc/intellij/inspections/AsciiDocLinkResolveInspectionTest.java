package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;

public class AsciiDocLinkResolveInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocLinkResolveInspection.class);
  }

  public void testWrongCaseForAnchor() {
    doTest(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  public void testFileDoesntExist() {
    doTestNoFix(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/linkResolve";
  }
}

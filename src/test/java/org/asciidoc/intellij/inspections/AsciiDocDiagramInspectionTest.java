package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;

public class AsciiDocDiagramInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocLinkResolveInspection.class);
  }

  public void testDuplicateDiagramName() {
    doTestNoFix(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/diagrams";
  }
}

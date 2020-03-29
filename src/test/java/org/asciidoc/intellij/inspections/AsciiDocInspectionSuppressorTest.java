package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;

public class AsciiDocInspectionSuppressorTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocLinkResolveInspection.class, AsciiDocReferencePatternInspection.class);
  }

  public void testSuppressionsWork() {
    doTestNoFix(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/suppressor";
  }
}

package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocChangeXrefWithNaturalCrossReferenceToId;

public class AsciiDocXrefWithNaturalCrossReferenceInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocXrefWithNaturalCrossReferenceInspection.class);
  }

  public void testXrefNaturalCrossReference() {
    doTest(AsciiDocChangeXrefWithNaturalCrossReferenceToId.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/xrefWithNaturalCrossReference";
  }
}

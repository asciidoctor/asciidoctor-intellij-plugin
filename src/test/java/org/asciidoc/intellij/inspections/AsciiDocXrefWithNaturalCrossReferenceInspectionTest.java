package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocChangeXrefWithNaturalCrossReferenceToId;

public class AsciiDocXrefWithNaturalCrossReferenceInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocChangeXrefWithNaturalCrossReferenceToId().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // noinspection unchecked
    myFixture.enableInspections(AsciiDocXrefWithNaturalCrossReferenceInspection.class);
  }

  public void testXrefNaturalCrossReference() {
    doTest(NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/xrefWithNaturalCrossReference";
  }
}

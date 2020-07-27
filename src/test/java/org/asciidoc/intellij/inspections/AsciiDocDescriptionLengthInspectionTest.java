package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;

public class AsciiDocDescriptionLengthInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocDescriptionLengthInspection.class);
  }

  public void testDescriptionIsTooLong() {
    doTestNoFix(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/descriptionLength";
  }
}

package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocAddBlockIdToSection;

/**
 * @author Alexander Schwartz
 */
public class AsciiDocAnchorWithoutIdInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocAnchorWithoutIdInspection.class);
  }

  public void testNoId() {
    doTest(AsciiDocAddBlockIdToSection.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/anchorWithoutId";
  }
}

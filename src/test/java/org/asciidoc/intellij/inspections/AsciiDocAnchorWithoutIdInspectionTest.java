package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocAddBlockIdToSection;

/**
 * @author Alexander Schwartz
 */
public class AsciiDocAnchorWithoutIdInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocAddBlockIdToSection().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // noinspection unchecked
    myFixture.enableInspections(AsciiDocAnchorWithoutIdInspection.class);
  }

  public void testNoId() {
    doTest(NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/anchorWithoutId";
  }
}

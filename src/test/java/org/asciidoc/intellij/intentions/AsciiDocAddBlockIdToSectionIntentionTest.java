package org.asciidoc.intellij.intentions;

import org.asciidoc.intellij.inspections.AsciiDocQuickFixTestBase;

/**
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocAddBlockIdToSectionIntentionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  public void testSubsection() {
    doTest("Add Block ID to section", false);
  }

  @Override
  protected String getBasePath() {
    return "intentions/blockIdToSection";
  }
}

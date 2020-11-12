package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocChangeToInlineMacro;

public class AsciiDocBlockMacroShouldBeInlineMacroInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocBlockMacroShouldBeInlineMacroInspection.class);
  }

  public void testXrefBlockMacroFix() {
    doTest(AsciiDocChangeToInlineMacro.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/blockMacroShouldBeInlineMacro";
  }
}

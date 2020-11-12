package org.asciidoc.intellij.inspections;

public class AsciiDocInlineMacroShouldBeBlockOrPreprocessorMacroInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocInlineMacroShouldBeBlockOrPreprocessorMacroInspection.class);
  }

  public void testIncludeInlineMacroFix() {
    doTest("Change to preprocessor macro", true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/inlineMacroShouldBeBlockOrPreprocessorMacro";
  }
}

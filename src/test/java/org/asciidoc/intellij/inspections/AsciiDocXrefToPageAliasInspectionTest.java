package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocChangeXrefFromPageAliasToPage;

public class AsciiDocXrefToPageAliasInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocChangeXrefFromPageAliasToPage().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocXrefToPageAliasInspection.class);
  }

  public void testXrefToAlias() {
    String testName = getTestName(true);
    myFixture.configureByFiles("modules/ROOT/pages/" + testName + ".adoc", "antora.yml", "modules/ROOT/pages/newpage.adoc");
    myFixture.checkHighlighting();
    applySingleQuickFix(NAME);
    myFixture.checkResultByFile("modules/ROOT/pages/" + testName + "_after.adoc", true);
  }

  public void testXrefToAliasInOtherModule() {
    myFixture.configureByFiles("modules/other/pages/xrefToAliasOtherModule.adoc", "antora.yml", "modules/ROOT/pages/newpage.adoc");
    myFixture.checkHighlighting();
    applySingleQuickFix(NAME);
    myFixture.checkResultByFile("modules/other/pages/xrefToAliasOtherModule_after.adoc", true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/xrefToPageAlias";
  }
}

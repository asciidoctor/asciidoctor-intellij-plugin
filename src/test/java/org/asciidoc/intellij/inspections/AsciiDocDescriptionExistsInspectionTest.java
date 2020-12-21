package org.asciidoc.intellij.inspections;

public class AsciiDocDescriptionExistsInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocDescriptionExistsInspection.class);
  }

  public void testAddMissingDescriptionJustTitle() {
    myFixture.testHighlighting("modules/ROOT/pages/justTitle.adoc", "antora.yml");
    applySingleQuickFix("Add page attribute description");
    myFixture.checkResultByFile("modules/ROOT/pages/justTitle-after.adoc", true);
  }

  public void testAddMissingDescriptionTitleWithExistingAttributes() {
    myFixture.testHighlighting("modules/ROOT/pages/titleWithAttributes.adoc", "antora.yml");
    applySingleQuickFix("Add page attribute description");
    myFixture.checkResultByFile("modules/ROOT/pages/titleWithAttributes-after.adoc", true);
  }

  public void testAddMissingDescriptionTitleWithWithText() {
    myFixture.testHighlighting("modules/ROOT/pages/titleWithText.adoc", "antora.yml");
    applySingleQuickFix("Add page attribute description");
    myFixture.checkResultByFile("modules/ROOT/pages/titleWithText-after.adoc", true);
  }

  public void testDescriptionExists() {
    // show that existing description is recognized
    myFixture.testHighlighting("modules/ROOT/pages/descriptionExists.adoc", "antora.yml");
  }

  @Override
  protected String getBasePath() {
    return "inspections/descriptionExists";
  }
}

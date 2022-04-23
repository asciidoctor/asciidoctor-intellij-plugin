package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocChangeToPassthrough;

public class AsciiDocAttributeUndefinedInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocChangeToPassthrough().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocAttributeUndefinedInspection.class);
  }

  public void testAttributeUndefined() {
    doTestNoFix(NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/attributeUndefined";
  }
}

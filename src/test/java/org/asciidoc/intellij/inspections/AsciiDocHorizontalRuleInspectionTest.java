package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocConvertMarkdownHorizontalRule;

public class AsciiDocHorizontalRuleInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocConvertMarkdownHorizontalRule().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // noinspection unchecked
    myFixture.enableInspections(AsciiDocHorizontalRuleInspection.class);
  }

  public void testMarkdownHorizontalRuleDash() {
    doTest(NAME, true);
  }

  public void testMarkdownHorizontalRuleStar() {
    doTest(NAME, true);
  }

  public void testMarkdownHorizontalRuleUnderscore() {
    doTest(NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/horizontalrule";
  }
}

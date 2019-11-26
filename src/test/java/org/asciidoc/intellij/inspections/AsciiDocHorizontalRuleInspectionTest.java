package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocConvertMarkdownHorizontalRule;

public class AsciiDocHorizontalRuleInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocHorizontalRuleInspection.class);
  }

  public void testMarkdownHorizontalRuleDash() {
    doTest(AsciiDocConvertMarkdownHorizontalRule.NAME, true);
  }

  public void testMarkdownHorizontalRuleStar() {
    doTest(AsciiDocConvertMarkdownHorizontalRule.NAME, true);
  }

  public void testMarkdownHorizontalRuleUnderscore() {
    doTest(AsciiDocConvertMarkdownHorizontalRule.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/horizontalrule";
  }
}

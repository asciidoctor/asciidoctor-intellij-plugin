package org.asciidoc.intellij.intentions;

import org.asciidoc.intellij.inspections.AsciiDocQuickFixTestBase;

/**
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocAdmonitionIntentionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  public void testMultiline() {
    doTest("Refactor to block admonition", false);
  }

  @Override
  protected String getBasePath() {
    return "intentions/admonition";
  }
}

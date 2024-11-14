package org.asciidoc.intellij.intentions;

import org.asciidoc.intellij.inspections.AsciiDocQuickFixTestBase;

/**
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocIgnoreValidationIntentionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  public void testNoOptions() {
    doTest("Hide errors in source blocks for this block", false);
  }

  public void testWithOptions() {
    doTest("Hide errors in source blocks for this block", false);
  }

  @Override
  protected String getBasePath() {
    return "intentions/ignoreValidation";
  }
}

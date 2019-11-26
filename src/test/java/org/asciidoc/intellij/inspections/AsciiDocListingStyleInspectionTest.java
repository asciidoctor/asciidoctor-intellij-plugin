package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocConvertMarkdownListing;

/**
 * @author Fatih Bozik
 */
public class AsciiDocListingStyleInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocListingStyleInspection.class);
  }

  public void testMarkdownListing() {
    doTest(AsciiDocConvertMarkdownListing.NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/listing";
  }
}

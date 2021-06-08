package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocConvertMarkdownListing;

/**
 * @author Fatih Bozik
 */
public class AsciiDocListingStyleInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocConvertMarkdownListing().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocListingStyleInspection.class);
  }

  public void testMarkdownListing() {
    doTest(NAME, true);
  }

  public void testMarkdownListingWithTitle() {
    doTest(NAME, true);
  }

  @Override
  protected String getBasePath() {
    return "inspections/listing";
  }
}

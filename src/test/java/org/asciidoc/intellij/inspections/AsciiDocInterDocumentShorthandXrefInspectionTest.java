package org.asciidoc.intellij.inspections;

import org.asciidoc.intellij.quickfix.AsciiDocChangeShorthandXrefToXrefMacro;

public class AsciiDocInterDocumentShorthandXrefInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocChangeShorthandXrefToXrefMacro().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocInterDocumentShorthandXrefInspection.class);
  }

  public void testShorthand() {
    myFixture.testHighlighting("shorthand.adoc");
    // the single quick fix only changes the first reference, so apply all fixes one by one
    for (int i = 0; i < 3; ++i) {
      var matcher = java.util.regex.Pattern.compile("<<[^<>#,]+#").matcher(myFixture.getEditor().getDocument().getText());
      assertTrue(matcher.find());
      myFixture.getEditor().getCaretModel().moveToOffset(matcher.start() + 2);
      myFixture.doHighlighting();
      myFixture.launchAction(myFixture.findSingleIntention(NAME));
    }
    myFixture.checkResultByFile("shorthand_after.adoc", true);
  }

  public void testNoFixIfTextContainsClosingBracket() {
    myFixture.configureByText("test.adoc", "See <<other.adoc#anchor,a [b] c>>.\n");
    myFixture.getEditor().getCaretModel().moveToOffset(myFixture.getFile().getText().indexOf("other"));
    myFixture.doHighlighting();
    assertEmpty(myFixture.filterAvailableIntentions(NAME));
    assertEquals(1, myFixture.doHighlighting().stream().filter(h -> h.getDescription() != null
      && h.getDescription().startsWith("Use the xref macro")).count());
  }

  @Override
  protected String getBasePath() {
    return "inspections/interDocumentShorthandXref";
  }
}

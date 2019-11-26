package org.asciidoc.intellij.psi;

import com.intellij.lang.LanguageASTFactory;
import com.intellij.testFramework.ParsingTestCase;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.AsciiDocTestingUtil;
import org.asciidoc.intellij.highlighting.AsciiDocColorSettingsPage;
import org.asciidoc.intellij.parser.AsciiDocParserDefinition;

import java.io.IOException;

/**
 * This class parses an adoc file and compares the output to a golden master result.
 * Look at /testdata/parser for the files; they match the test case.
 */
public class AsciiDocParserTest extends ParsingTestCase {

  public AsciiDocParserTest() {
    super("parser", "adoc", true, new AsciiDocParserDefinition());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addExplicitExtension(LanguageASTFactory.INSTANCE, AsciiDocLanguage.INSTANCE, new AsciiDocASTFactory());
  }

  @Override
  protected String getTestDataPath() {
    return AsciiDocTestingUtil.TEST_DATA_PATH;
  }

  public void testColorsAndFontsSample() throws IOException {
    final AsciiDocColorSettingsPage colorSettingsPage = new AsciiDocColorSettingsPage();
    String demoText = colorSettingsPage.getDemoText();
    for (String tag : colorSettingsPage.getAdditionalHighlightingTagToDescriptorMap().keySet()) {
      demoText = demoText.replaceAll("<" + tag + ">", "");
      demoText = demoText.replaceAll("</" + tag + ">", "");
    }
    doCodeTest(demoText);
  }

  public void testSectionsWithPreBlock() {
    doTest(true);
  }

  public void testListingWithCodeBlock() {
    doTest(true);
  }

  public void testInlineLinks() {
    doTest(true);
  }
}

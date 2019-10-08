package org.asciidoc.intellij.formatting;

import com.intellij.codeInsight.actions.FileInEditorProcessor;
import com.intellij.codeInsight.actions.LayoutCodeOptions;
import com.intellij.codeInsight.actions.ReformatCodeRunOptions;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;

import java.io.File;

import static com.intellij.codeInsight.actions.TextRangeType.WHOLE_FILE;

public class AsciiDocFormattingModelBuilderTest extends LightPlatformCodeInsightFixtureTestCase {

  @Override
  protected String getTestDataPath() {
    return new File("testData/" + getBasePath()).getAbsolutePath() + "/actions/reformatFileInEditor/";
  }

  private void doTest(LayoutCodeOptions options) {
    myFixture.configureByFile(getTestName(true) + "_before.adoc");
    FileInEditorProcessor processor = new FileInEditorProcessor(myFixture.getFile(), myFixture.getEditor(), options);
    processor.processCode();
    myFixture.checkResultByFile(getTestName(true) + "_after.adoc");
  }

  public void testHeadings() {
    doTest(new ReformatCodeRunOptions(WHOLE_FILE));
  }

  public void testEnumerations() {
    doTest(new ReformatCodeRunOptions(WHOLE_FILE));
  }

  public void testBlocks() {
    doTest(new ReformatCodeRunOptions(WHOLE_FILE));
  }

}

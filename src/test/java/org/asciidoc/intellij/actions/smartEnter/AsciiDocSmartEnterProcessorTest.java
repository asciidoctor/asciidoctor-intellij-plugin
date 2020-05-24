package org.asciidoc.intellij.actions.smartEnter;

import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor;
import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessors;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.AsciiDocTestingUtil;

import java.util.List;

public class AsciiDocSmartEnterProcessorTest extends BasePlatformTestCase {

  @Override
  protected String getTestDataPath() {
    return AsciiDocTestingUtil.TEST_DATA_PATH;
  }

  public void doTest() {
    myFixture.configureByFile("smartEnter/" + getTestName(true) + ".adoc");
    final List<SmartEnterProcessor> processors = SmartEnterProcessors.INSTANCE.forKey(AsciiDocLanguage.INSTANCE);
    WriteCommandAction.runWriteCommandAction(myFixture.getProject(), () -> {
      final Editor editor = myFixture.getEditor();
      for (SmartEnterProcessor processor : processors) {
        processor.process(myFixture.getProject(), editor, myFixture.getFile());
      }
    });
    myFixture.checkResultByFile("smartEnter/" + getTestName(true) + "_after.adoc", true);
  }

  public void testClosingBlockAttributes() {
    doTest();
  }

  public void testBoundaryAutocomplete() {
    doTest();
  }

  public void testSourceBlockAutocomplete() {
    doTest();
  }

  public void testSourceBlockAutocompleteInsideBracket() {
    doTest();
  }

  public void testPlantumlBlockAutocomplete() {
    doTest();
  }

  public void testClosingBlockMacro() {
    doTest();
  }

  public void testIncludeLevel() {
    doTest();
  }

  public void testXrefTitle() {
    doTest();
  }

  public void testXrefTitleAfterBracket() {
    doTest();
  }

  public void testXrefTitleWithAnchor() {
    doTest();
  }

  public void testXrefTitleWithAnchorToBlockId() {
    doTest();
  }
}

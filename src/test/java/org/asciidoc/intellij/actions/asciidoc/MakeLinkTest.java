package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor;
import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessors;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.AsciiDocTestingUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MakeLinkTest extends LightPlatformCodeInsightTestCase {

  @NotNull
  @Override
  protected String getTestDataPath() {
    return AsciiDocTestingUtil.TEST_DATA_PATH;
  }

  public void doTest() {
    configureByFile("/actions/makeLink/" + getTestName(true) + ".adoc");
    final List<SmartEnterProcessor> processors = SmartEnterProcessors.INSTANCE.forKey(AsciiDocLanguage.INSTANCE);
    executeAction("asciidoc.makelink");
    checkResultByFile("/actions/makeLink/" + getTestName(true) + "_after.adoc");
  }

  public void testMakeLinkFromText() {
    doTest();
  }

  public void testMakeLinkFromLink() {
    doTest();
  }

  public void testMakeLinkFromLinkInBrackets() {
    doTest();
  }

  public void testMakeLinkFromEmail() {
    doTest();
  }

}

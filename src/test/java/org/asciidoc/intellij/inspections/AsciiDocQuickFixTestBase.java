package org.asciidoc.intellij.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AsciiDocQuickFixTestBase extends AsciiDocCodeInsightFixtureTestCase {
  protected void doTest(@NotNull String quickFixName) {
    doTest(quickFixName, false);
  }

  protected void doTest(@NotNull String quickFixName, boolean checkHighlighting) {
    String testName = getTestName(true);
    configure(checkHighlighting, testName);
    applySingleQuickFix(quickFixName);
    myFixture.checkResultByFile(testName + "-after.adoc", true);
  }

  protected void doTest(@NotNull Class<? extends IntentionAction> intentionAction, boolean checkHighlighting) {
    String testName = getTestName(true);
    configure(checkHighlighting, testName);
    applySingleQuickFix(intentionAction);
    myFixture.checkResultByFile(testName + "-after.adoc", true);
  }

  protected void doTestNoFix(@NotNull String name) {
    doTestNoFix(name, false);
  }

  protected void doTestNoFix(@NotNull String name, boolean checkHighlighting) {
    configure(checkHighlighting, getTestName(true));
    List<IntentionAction> availableIntentions = myFixture.filterAvailableIntentions(name);
    assertEmpty(availableIntentions);
  }

  private void configure(boolean checkHighlighting, String testName) {
    if (checkHighlighting) {
      myFixture.testHighlighting(testName + ".adoc");
    } else {
      myFixture.configureByFile(testName + ".adoc");
      myFixture.doHighlighting();
    }
  }
}

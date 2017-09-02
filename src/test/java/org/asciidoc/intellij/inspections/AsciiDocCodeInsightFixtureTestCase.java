package org.asciidoc.intellij.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

abstract public class AsciiDocCodeInsightFixtureTestCase extends LightPlatformCodeInsightFixtureTestCase {

  @Override
  protected boolean isWriteActionRequired() {
    return false;
  }

  @Override
  protected final String getTestDataPath() {
    return new File("testData/" + getBasePath()).getAbsolutePath();
  }

  protected void applySingleQuickFix(@NotNull String quickFixName) {
    List<IntentionAction> availableIntentions = myFixture.filterAvailableIntentions(quickFixName);
    IntentionAction action = ContainerUtil.getFirstItem(availableIntentions);
    assertNotNull(action);
    myFixture.launchAction(action);
  }
}

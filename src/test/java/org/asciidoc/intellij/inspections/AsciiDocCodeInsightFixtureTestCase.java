package org.asciidoc.intellij.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AsciiDocCodeInsightFixtureTestCase extends BasePlatformTestCase {

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

  protected void applySingleQuickFix(@NotNull Class<? extends IntentionAction> clazz) {
    List<IntentionAction> availableIntentions = myFixture.getAllQuickFixes().stream()
      .filter(intentionAction -> clazz.isAssignableFrom(intentionAction.getClass()))
      .collect(Collectors.toList());
    IntentionAction action = ContainerUtil.getFirstItem(availableIntentions);
    assertNotNull(action);
    myFixture.launchAction(action);
  }
}

package org.asciidoc.intellij.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for code inspection test cases.
 * The Problem with <code>com.intellij.grazie.pro</code> is described here:
 * <a href="https://youtrack.jetbrains.com/issue/IDEA-205964">IDEA-205964</a> and
 * <a href="https://youtrack.jetbrains.com/issue/GRZ-504/">GRZ-504</a>.
 */
public abstract class AsciiDocCodeInsightFixtureTestCase extends BasePlatformTestCase {

  @Override
  protected boolean isWriteActionRequired() {
    return false;
  }

  @Override
  protected final String getTestDataPath() {
    return new File("build/resources/test/testData/" + getBasePath()).getAbsolutePath();
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

package org.asciidoc.intellij.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.ide.plugins.PluginEnabler;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AsciiDocCodeInsightFixtureTestCase extends BasePlatformTestCase {

  static {
    Set<PluginId> disabled = new HashSet<>();

    // to avoid:  java.lang.NoClassDefFoundError: Could not initialize class ai.grazie.nlp.tokenizer.spacy.SpacyBaseLanguage
    disabled.add(PluginId.getId("com.intellij.grazie.pro"));
    disabled.add(PluginId.getId("tanvd.grazi"));

    // to improve performance, remove plugins used for debugging in interactive mode
    disabled.add(PluginId.getId("PsiViewer"));
    disabled.add(PluginId.getId("PlantUML integration"));
    disabled.add(PluginId.getId("com.intellij.platform.images"));
    disabled.add(PluginId.getId("com.intellij.javafx"));

    PluginEnabler.HEADLESS.disableById(disabled);
  }

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

package org.asciidoc.intellij.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;
import org.assertj.core.api.Assertions;

import java.util.List;

public class AsciiDocAntoraModuleResolveInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocChangeCaseForAnchor().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocAntoraModuleResolveInspection.class);
  }

  public void testModuleDoesNotResolve() {
    doTestNoQuickfix();
  }

  protected void doTestNoQuickfix() {
    String testName = getTestName(true);
    myFixture.testHighlighting("modules/ROOT/pages/" + testName + ".adoc", "antora.yml");
    List<IntentionAction> availableIntentions = myFixture.filterAvailableIntentions(NAME);
    Assertions.assertThat(availableIntentions).isEmpty();
  }

  @Override
  protected String getBasePath() {
    return "inspections/antoraModuleResolve";
  }
}

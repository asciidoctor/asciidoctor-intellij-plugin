package org.asciidoc.intellij.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import org.asciidoc.intellij.quickfix.AsciiDocAddAdocExtensionToXref;
import org.assertj.core.api.Assertions;

import java.util.List;

public class AsciiDocXrefWithoutExtensionInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocXrefWithoutExtensionInspection.class);
  }

  public void testXrefAddingExtension() {
    doTestWithQuickfix();
  }

  public void testXrefNoExtensionWithAttribute() {
    doTestNoQuickfix();
  }

  protected void doTestWithQuickfix() {
    String testName = getTestName(true);
    myFixture.testHighlighting("modules/ROOT/pages/" + testName + ".adoc", "antora.yml");
    applySingleQuickFix(AsciiDocAddAdocExtensionToXref.NAME);
    myFixture.checkResultByFile("modules/ROOT/pages/" + testName + "_after.adoc", true);
  }

  protected void doTestNoQuickfix() {
    String testName = getTestName(true);
    myFixture.testHighlighting("modules/ROOT/pages/" + testName + ".adoc", "antora.yml");
    List<IntentionAction> availableIntentions = myFixture.filterAvailableIntentions(AsciiDocAddAdocExtensionToXref.NAME);
    Assertions.assertThat(availableIntentions).isEmpty();
  }

  @Override
  protected String getBasePath() {
    return "inspections/xrefWithoutExtension";
  }
}

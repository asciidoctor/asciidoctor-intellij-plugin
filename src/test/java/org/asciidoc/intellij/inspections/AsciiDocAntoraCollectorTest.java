package org.asciidoc.intellij.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;
import org.assertj.core.api.Assertions;

import java.util.List;

public class AsciiDocAntoraCollectorTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocChangeCaseForAnchor().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocAntoraModuleResolveInspection.class, AsciiDocLinkResolveInspection.class, AsciiDocAttributeUndefinedInspection.class);
  }

  public void testAntoraComponentOverride() {
    doTestNoQuickfix();
  }
  public void testGeneratedPage() {
    doTestNoQuickfix();
    AsciiDocLink link = PsiTreeUtil.findChildOfType(myFixture.getFile(), AsciiDocLink.class);
    Assertions.assertThat(link.getReferences()[0].resolve()).isNotNull();
  }
  public void testGeneratedPageWithBase() {
    doTestNoQuickfix();
    AsciiDocLink link = PsiTreeUtil.findChildOfType(myFixture.getFile(), AsciiDocLink.class);
    Assertions.assertThat(link.getReferences()[0].resolve()).isNotNull();
  }

  protected void doTestNoQuickfix() {
    String testName = getTestName(true);
    myFixture.testHighlighting("modules/ROOT/pages/" + testName + ".adoc", "antora.yml",
      "collector-component/antora.yml", "collector-page/modules/ROOT/pages/generated-page.adoc", "collector-page-with-base/generated-page-with-base.adoc");
    List<IntentionAction> availableIntentions = myFixture.filterAvailableIntentions(NAME);
    Assertions.assertThat(availableIntentions).isEmpty();
  }

  @Override
  protected String getBasePath() {
    return "inspections/antoraCollector";
  }
}

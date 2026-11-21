package org.asciidoc.intellij.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;
import org.assertj.core.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

  public void testGeneratedPageFromList() {
    doTestNoQuickfix();
    assertLinkResolves(true);
  }

  public void testGeneratedPageFromString() {
    doTestNoQuickfix();
    assertLinkResolves(true);
  }

  public void testGeneratedPageFilesMatch() {
    doTestNoQuickfix();
    assertLinkResolves(true);
  }

  public void testGeneratedPageFilesNoMatch() {
    configure();
    assertLinkResolves(false);
  }

  public void testScanDirectoryOutsideOfRepositoryIsIgnored() throws IOException {
    File outside = FileUtil.createTempDirectory("collector-outside", null, true);
    File outsidePage = new File(outside, "modules/ROOT/pages/outside-page.adoc");
    FileUtil.writeToFile(outsidePage, "= Outside page\n");
    myFixture.addFileToProject("collector-inside/modules/ROOT/pages/inside-page.adoc", "= Inside page\n");
    myFixture.addFileToProject("antora.yml", "name: collector-outside\nversion: ~\next:\n  collector:\n    scan:\n      - dir: ./collector-inside\n"
      + "      - dir: " + outside.getAbsolutePath().replace('\\', '/') + "\n");
    myFixture.configureFromExistingVirtualFile(myFixture.addFileToProject("modules/ROOT/pages/test.adoc",
      "xref:inside-page.adoc[]\n\nxref:outside-page.adoc[]\n").getVirtualFile());
    List<AsciiDocLink> links = new ArrayList<>(PsiTreeUtil.findChildrenOfType(myFixture.getFile(), AsciiDocLink.class));
    Assertions.assertThat(links).hasSize(2);
    Assertions.assertThat(links.get(0).getReferences()[0].resolve()).isNotNull();
    Assertions.assertThat(links.get(1).getReferences()[0].resolve()).isNull();
  }

  private void configure() {
    String testName = getTestName(true);
    myFixture.configureByFiles("modules/ROOT/pages/" + testName + ".adoc", "antora.yml",
      "collector-component/antora.yml", "collector-page/modules/ROOT/pages/generated-page.adoc", "collector-page-with-base/generated-page-with-base.adoc",
      "collector-list-a/generated-list-a.adoc", "collector-list-b/modules/ROOT/pages/generated-string.adoc",
      "collector-files/match-page.adoc", "collector-files/other-page.adoc");
  }

  private void assertLinkResolves(boolean expected) {
    AsciiDocLink link = PsiTreeUtil.findChildOfType(myFixture.getFile(), AsciiDocLink.class);
    if (expected) {
      Assertions.assertThat(link.getReferences()[0].resolve()).isNotNull();
    } else {
      Assertions.assertThat(link.getReferences()[0].resolve()).isNull();
    }
  }

  protected void doTestNoQuickfix() {
    String testName = getTestName(true);
    myFixture.testHighlighting("modules/ROOT/pages/" + testName + ".adoc", "antora.yml",
      "collector-component/antora.yml", "collector-page/modules/ROOT/pages/generated-page.adoc", "collector-page-with-base/generated-page-with-base.adoc",
      "collector-list-a/generated-list-a.adoc", "collector-list-b/modules/ROOT/pages/generated-string.adoc",
      "collector-files/match-page.adoc", "collector-files/other-page.adoc");
    List<IntentionAction> availableIntentions = myFixture.filterAvailableIntentions(NAME);
    Assertions.assertThat(availableIntentions).isEmpty();
  }

  @Override
  protected String getBasePath() {
    return "inspections/antoraCollector";
  }
}

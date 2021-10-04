package org.asciidoc.intellij.inspections;

import com.intellij.psi.PsiDirectory;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFileQuickfix;
import org.junit.Assert;

public class AsciiDocLinkResolveInspectionTest extends AsciiDocQuickFixTestBase {

  private static final String NAME = new AsciiDocChangeCaseForAnchor().getName();

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // noinspection unchecked
    myFixture.enableInspections(AsciiDocLinkResolveInspection.class);
  }

  public void testWrongCaseForAnchor() {
    doTest(NAME, true);
  }

  public void testFileDoesntExist() {
    doTestNoFix(NAME, true);
  }

  public void testLocalAnchor() {
    doTestNoFix(NAME, true);
  }

  public void testCreateMissingXrefFile() {
    doTest(new AsciiDocCreateMissingFileQuickfix().getName(), true);
    PsiDirectory parent = myFixture.getFile().getParent();
    Assert.assertNotNull(parent);
    PsiDirectory subdir = parent.findSubdirectory("ab");
    Assert.assertNotNull(subdir);
    Assert.assertNotNull(subdir.findFile("missing.adoc"));
  }

  @Override
  protected String getBasePath() {
    return "inspections/linkResolve";
  }
}

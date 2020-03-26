package org.asciidoc.intellij.inspections;

import com.intellij.psi.PsiDirectory;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFileQuickfix;
import org.junit.Assert;

public class AsciiDocLinkResolveInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocLinkResolveInspection.class);
  }

  public void testWrongCaseForAnchor() {
    doTest(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  public void testFileDoesntExist() {
    doTestNoFix(AsciiDocChangeCaseForAnchor.NAME, true);
  }

  public void testCreateMissingXrefFile() {
    doTest(AsciiDocCreateMissingFileQuickfix.NAME, true);
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

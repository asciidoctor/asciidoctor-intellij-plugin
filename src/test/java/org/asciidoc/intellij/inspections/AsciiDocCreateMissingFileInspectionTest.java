package org.asciidoc.intellij.inspections;

import com.intellij.psi.PsiDirectory;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFile;
import org.junit.Assert;

public class AsciiDocCreateMissingFileInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
    myFixture.enableInspections(AsciiDocPageBreakInspection.class);
  }

  public void testCreateMissingIncludeFile() {
    doTest(AsciiDocCreateMissingFile.class, true);
    PsiDirectory parent = myFixture.getFile().getParent();
    Assert.assertNotNull(parent);
    PsiDirectory subdir = parent.findSubdirectory("ab");
    Assert.assertNotNull(subdir);
    Assert.assertNotNull(subdir.findFile("missing.adoc"));
  }

  @Override
  protected String getBasePath() {
    return "inspections/missingfile";
  }
}

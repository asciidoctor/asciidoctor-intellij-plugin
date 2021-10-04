package org.asciidoc.intellij.inspections;

import com.intellij.psi.PsiDirectory;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFileIntentionAction;
import org.junit.Assert;

public class AsciiDocCreateMissingFileInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // noinspection unchecked
    myFixture.enableInspections(AsciiDocPageBreakInspection.class);
  }

  public void testCreateMissingIncludeFile() {
    doTest(AsciiDocCreateMissingFileIntentionAction.class, true);
    PsiDirectory parent = myFixture.getFile().getParent();
    Assert.assertNotNull(parent);
    PsiDirectory subdir = parent.findSubdirectory("ab");
    Assert.assertNotNull(subdir);
    Assert.assertNotNull(subdir.findFile("missing.adoc"));
  }

  @Override
  protected String getBasePath() {
    // highlighting contains a "C:" in the path name on windows, therefore different fixtures
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      return "inspections/missingfile-win";
    } else {
      return "inspections/missingfile";
    }
  }
}

package org.asciidoc.intellij.inspections;

import com.intellij.psi.PsiDirectory;
import org.apache.commons.io.FileUtils;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFileIntentionAction;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AsciiDocCreateMissingFileInspectionTest extends AsciiDocQuickFixTestBase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    //noinspection unchecked
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
    try {
      File path = new File("build/testdata/missingfile");
      FileUtils.deleteDirectory(path);
      FileUtils.forceMkdir(path);
      FileUtils.copyDirectory(new File("testdata/inspections/missingfile"), new File("build/testdata/missingfile"));
      if (System.getProperty("os.name").toLowerCase().contains("win")) {
        // highlighting contains a "C:" in the path name on windows, therefore the fixture needs to be patched
        String contents = FileUtils.readFileToString(new File("build/testdata/missingfile/createMissingIncludeFile.adoc"), StandardCharsets.UTF_8);
        contents = contents.replaceAll("/src", new File("build/testdata/missingfile/createMissingIncludeFile.adoc").getAbsolutePath().charAt(0) + ":" + "/src");
        FileUtils.writeStringToFile(new File("build/testdata/missingfile/createMissingIncludeFile.adoc"), contents, StandardCharsets.UTF_8);
      }
      return "../build/testdata/missingfile";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

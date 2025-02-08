package org.asciidoc.intellij.inspections;

import com.intellij.psi.PsiDirectory;
import org.apache.commons.io.FileUtils;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFileIntentionAction;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

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
      File path = new File("build/testData/missingfile");
      FileUtils.deleteDirectory(path);
      FileUtils.forceMkdir(path);
      FileUtils.copyDirectory(new File("src/test/resources/testData/inspections/missingfile"), new File("build/testData/missingfile"));
      if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
        // highlighting contains a "C:" in the path name on windows, therefore the fixture needs to be patched
        String contents = FileUtils.readFileToString(new File("build/testData/missingfile/createMissingIncludeFile.adoc"), StandardCharsets.UTF_8);
        contents = contents.replaceAll("/src", new File("build/testData/missingfile/createMissingIncludeFile.adoc").getAbsolutePath().charAt(0) + ":" + "/src");
        FileUtils.writeStringToFile(new File("build/testData/missingfile/createMissingIncludeFile.adoc"), contents, StandardCharsets.UTF_8);
      }
      return "../../../testData/missingfile";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

package org.asciidoc.intellij.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class AsciiDocMoveFileTest extends BasePlatformTestCase {

  private void move(String file, String targetDir) {
    PsiFile psiFile = myFixture.getFile().getManager().findFile(myFixture.findFileInTempDir(file));
    PsiDirectory directory = myFixture.getPsiManager().findDirectory(myFixture.findFileInTempDir(targetDir));
    ApplicationManager.getApplication().invokeAndWait(() ->
      new MoveFilesOrDirectoriesProcessor(getProject(), new com.intellij.psi.PsiElement[]{psiFile}, directory,
        true, false, false, null, null).run());
  }

  private void setUpAntora(String macro) {
    myFixture.addFileToProject("antora.yml", "name: test\nversion: '1.0'\n");
    myFixture.addFileToProject("modules/ROOT/pages/move-me.adoc", "Content\n");
    myFixture.addFileToProject("modules/ROOT/partials/placeholder.adoc", "Placeholder\n");
    myFixture.addFileToProject("modules/other/pages/placeholder.adoc", "Placeholder\n");
    myFixture.configureFromExistingVirtualFile(
      myFixture.addFileToProject("modules/ROOT/pages/index.adoc", macro + "\n").getVirtualFile());
  }

  public void testMoveIncludedPageToPartials() {
    setUpAntora("include::move-me.adoc[]");
    move("modules/ROOT/pages/move-me.adoc", "modules/ROOT/partials");
    assertEquals("include::partial$move-me.adoc[]\n", myFixture.getFile().getText());
  }

  public void testMoveIncludedPartialBackToPages() {
    setUpAntora("include::partial$placeholder.adoc[]");
    move("modules/ROOT/partials/placeholder.adoc", "modules/ROOT/pages");
    assertEquals("include::placeholder.adoc[]\n", myFixture.getFile().getText());
  }

  public void testMoveIncludedPartialToOtherModule() {
    setUpAntora("include::partial$placeholder.adoc[]");
    myFixture.addFileToProject("modules/other/partials/keep.adoc", "Keep\n");
    move("modules/ROOT/partials/placeholder.adoc", "modules/other/partials");
    assertEquals("include::other:partial$placeholder.adoc[]\n", myFixture.getFile().getText());
  }

  public void testMovePageToOtherModule() {
    setUpAntora("xref:move-me.adoc[]");
    move("modules/ROOT/pages/move-me.adoc", "modules/other/pages");
    assertEquals("xref:other:move-me.adoc[]\n", myFixture.getFile().getText());
  }
}

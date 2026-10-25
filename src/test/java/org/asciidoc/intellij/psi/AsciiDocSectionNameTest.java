package org.asciidoc.intellij.psi;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.concurrent.TimeUnit;

public class AsciiDocSectionNameTest extends BasePlatformTestCase {

  public void testGetNameWithoutReadActionOnBackgroundThread() throws Exception {
    PsiFile file = myFixture.configureByText("test.adoc", "== Heading\n\nText");
    AsciiDocSection section = PsiTreeUtil.findChildOfType(file, AsciiDocSection.class);
    assertNotNull(section);
    // Search Everywhere's ML ranking calls this without a read action, which used to fail
    String name = ApplicationManager.getApplication().executeOnPooledThread(section::getName).get(10, TimeUnit.SECONDS);
    assertEquals("Heading", name);
  }
}

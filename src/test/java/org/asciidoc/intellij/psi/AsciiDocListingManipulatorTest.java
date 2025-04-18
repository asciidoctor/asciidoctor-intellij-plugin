package org.asciidoc.intellij.psi;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.asciidoc.intellij.file.AsciiDocFileType;

public class AsciiDocListingManipulatorTest extends BasePlatformTestCase {
  public void testSimpleCodeFence() {
    doTest("""
        [source,json]
        ----
        hi<caret>ho
        ----
        """,
      "content",
      """
        [source,json]
        ----
        content
        ----""");
  }

  private void doTest(String text, String newContent, String expectedText) {
    myFixture.configureByText(AsciiDocFileType.INSTANCE, text);

    int offset = myFixture.getCaretOffset();

    PsiElement element = myFixture.getFile().findElementAt(offset);
    assertNotNull(element);

    AsciiDocListing codeFence = (AsciiDocListing) InjectedLanguageManager.getInstance(getProject()).getInjectionHost(element);
    assertNotNull(codeFence);

    TextRange range = codeFence.createLiteralTextEscaper().getRelevantTextRange();

    final AsciiDocListing.Manipulator manipulator = new AsciiDocListing.Manipulator();
    AsciiDocListing newCodeFence =
      WriteCommandAction.runWriteCommandAction(myFixture.getProject(), (Computable<AsciiDocListing>) () -> manipulator
        .handleContentChange(codeFence, range, newContent));

    assertEquals(expectedText, newCodeFence.getText());
  }
}

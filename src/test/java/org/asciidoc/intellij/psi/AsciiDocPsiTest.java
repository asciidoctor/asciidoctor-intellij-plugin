package org.asciidoc.intellij.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.asciidoc.intellij.file.AsciiDocFileType;

/**
 * @author yole
 */
public class AsciiDocPsiTest extends LightPlatformCodeInsightFixtureTestCase {
  public void testImageBlockMacro() {
    PsiFile psiFile = myFixture.configureByText(AsciiDocFileType.INSTANCE, "image::foo.png[Foo]");
    AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) psiFile.getChildren()[0];
    assertEquals("image", blockMacro.getMacroName());
    PsiReference[] references = blockMacro.getReferences();
    assertEquals(1, references.length);
  }

  public void testExampleBlock() {
    PsiFile psiFile = myFixture.configureByText(AsciiDocFileType.INSTANCE, "====\nfoo\n====\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
    assertInstanceOf(children[0], AsciiDocBlock.class);
  }

  public void testExampleBlockWithTitle() {
    PsiFile psiFile = myFixture.configureByText(AsciiDocFileType.INSTANCE, ".Xyzzy\n====\nfoo\n====\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
    AsciiDocBlock block = (AsciiDocBlock) children[0];
    assertEquals("Xyzzy", block.getTitle());
  }

  public void testImageBlockMacroWithTitle() {
    PsiFile psiFile = myFixture.configureByText(AsciiDocFileType.INSTANCE, ".Xyzzy\nimage::foo.png[]");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
    AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) children[0];
    assertEquals("image", blockMacro.getMacroName());
    assertEquals("Xyzzy", blockMacro.getTitle());
  }

  public void testBlockAttributes() {
    PsiFile psiFile = myFixture.configureByText(AsciiDocFileType.INSTANCE, "[NOTE]\n====\nfoo\n====\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
    AsciiDocBlock block = (AsciiDocBlock) children[0];
    assertEquals("NOTE", block.getStyle());
  }
}

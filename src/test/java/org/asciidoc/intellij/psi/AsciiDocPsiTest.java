package org.asciidoc.intellij.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.asciidoc.intellij.file.AsciiDocFileType;

/**
 * @author yole
 */
public class AsciiDocPsiTest extends LightPlatformCodeInsightFixtureTestCase {
  public void testImageBlockMacro() {
    PsiFile psiFile = configureByAsciiDoc("image::foo.png[Foo]");
    AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) psiFile.getChildren()[0];
    assertEquals("image", blockMacro.getMacroName());
    PsiReference[] references = blockMacro.getReferences();
    assertEquals(1, references.length);
  }

  public void testExampleBlock() {
    PsiFile psiFile = configureByAsciiDoc("====\nfoo\n====\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
    assertInstanceOf(children[0], AsciiDocBlock.class);
  }

  public void testExampleBlockWithTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Xyzzy\n====\nfoo\n====\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
    AsciiDocBlock block = (AsciiDocBlock) children[0];
    assertEquals("Xyzzy", block.getTitle());
  }

  public void testImageBlockMacroWithTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Xyzzy\nimage::foo.png[]");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
    AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) children[0];
    assertEquals("image", blockMacro.getMacroName());
    assertEquals("Xyzzy", blockMacro.getTitle());
  }

  public void testBlockAttributes() {
    PsiFile psiFile = configureByAsciiDoc("[NOTE]\n====\nfoo\n====\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
    AsciiDocBlock block = (AsciiDocBlock) children[0];
    assertEquals("NOTE", block.getStyle());
  }

  public void testSidebarBlockWithTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Xyzzy\n****\nfoo\n****\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
    AsciiDocBlock block = (AsciiDocBlock) children[0];
    assertEquals("Xyzzy", block.getTitle());
  }

  public void testListingBlockAttributesThenSidebar() {
    PsiFile psiFile = configureByAsciiDoc("[source]\n----\nfoo\n----\n<1> Bar\n\n.Title\n****\nFoo\n****\n");
    AsciiDocBlock[] blocks = PsiTreeUtil.getChildrenOfType(psiFile, AsciiDocBlock.class);
    assertEquals(2, blocks.length);
  }

  private PsiFile configureByAsciiDoc(String text) {
    return myFixture.configureByText(AsciiDocFileType.INSTANCE, text);
  }

}

package org.asciidoc.intellij.psi;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Tests for {@link org.asciidoc.intellij.parser.AsciiDocParserImpl}.
 * HINT: instead of this test, consider a golden master test in {@link AsciiDocParserTest}
 *
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
    assertEquals(2, children.length);
    assertInstanceOf(children[0], AsciiDocStandardBlock.class);
  }

  public void testExampleBlockWithTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Xyzzy\n====\nfoo\n====\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(2, children.length);
    AsciiDocStandardBlock block = (AsciiDocStandardBlock) children[0];
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

  public void testImageBlockMacroWithReferences() {
    PsiFile psiFile = configureByAsciiDoc("image::dir/foo.png[]");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
    AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) children[0];
    assertEquals(2, blockMacro.getReferences().length);
    assertEquals(AsciiDocFileReference.class, blockMacro.getReferences()[0].getClass());
    // parent folder should be visible
    assertEquals(2, blockMacro.getReferences()[0].getVariants().length);
    assertTrue(((LookupElementBuilder) blockMacro.getReferences()[0].getVariants()[0]).getAllLookupStrings().contains(".."));
  }

  public void testIfdefBlockMacroWithReferences() {
    PsiFile psiFile = configureByAsciiDoc(":hi: ho\nifdef::hi,ho+hu[]");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(3, children.length);
    AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) children[2];
    assertEquals(3, blockMacro.getChildren().length);
    AsciiDocAttributeReference ref = (AsciiDocAttributeReference) blockMacro.getChildren()[0];
    assertEquals(AsciiDocAttributeDeclarationReference.class, ref.getReferences()[0].getClass());
    assertEquals(1, ref.getReferences()[0].getVariants().length);
  }

  public void testIncompleteBlockMacroWithAttributesInNewLine() {
    PsiFile psiFile = configureByAsciiDoc("image::foo.png\n[hi]");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(3, children.length);
    assertEquals("image::foo.png", children[0].getText());
    assertEquals("\n", children[1].getText());
    assertEquals("[hi]", children[2].getText());
  }

  public void testEnumerationFollowedByText() {
    PsiFile psiFile = configureByAsciiDoc("[square]\n* Hi\n\n* Ho\n* http://www.gmx.net\n\nText");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(4, children.length);

    assertEquals(AsciiDocElementTypes.BLOCK, children[0].getNode().getElementType());

    assertEquals("\n", children[1].getText());

    assertEquals("\n", children[2].getText());

    assertEquals("Text", children[3].getText());
    assertEquals(AsciiDocTokenTypes.TEXT, children[3].getNode().getElementType());
  }

  public void testBlockAttributes() {
    PsiFile psiFile = configureByAsciiDoc("[NOTE]\n====\nfoo\n====\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(2, children.length);
    AsciiDocStandardBlock block = (AsciiDocStandardBlock) children[0];
    assertEquals("NOTE", block.getStyle());
  }

  public void testBlockMacroWithAttributes() {
    PsiFile psiFile = configureByAsciiDoc("foo::bar[foo='bar',foo=\"bar\",foo=bar,{foo}={bar}]");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(1, children.length);
  }

  public void testOldStyleHeader() {
    PsiFile psiFile = configureByAsciiDoc("H\n+\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(2, children.length);
    AsciiDocSection section = (AsciiDocSection) children[0];
    assertEquals("H", section.getName());
  }

  public void testBlockFollowedBySomethingLookingLikeAHeaderButActuallyPlainText() {
    PsiFile psiFile = configureByAsciiDoc("====\n\n==== Test\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(2, children.length);
    AsciiDocStandardBlock block = (AsciiDocStandardBlock) children[0];
    assertEquals("====\n\n==== Test", block.getText());
  }

  public void testSidebarBlockWithTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Xyzzy\n****\nfoo\n****\n");
    PsiElement[] children = psiFile.getChildren();
    assertEquals(2, children.length);
    AsciiDocStandardBlock block = (AsciiDocStandardBlock) children[0];
    assertEquals("Xyzzy", block.getTitle());
  }

  public void testListingBlockAttributesThenSidebar() {
    PsiFile psiFile = configureByAsciiDoc("[source]\n----\nfoo\n----\n<1> Bar\n\n.Title\n****\nFoo\n****\n");
    AsciiDocBlock[] blocks = PsiTreeUtil.getChildrenOfType(psiFile, AsciiDocBlock.class);
    assertNotNull(blocks);
    assertEquals(2, blocks.length);
  }

  public void testListingWithBlankAfterDelimiter() {
    PsiFile psiFile = configureByAsciiDoc("[source]\n---- \nfoo\n----\nText");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertTrue(psiFile.getChildren()[1] instanceof PsiWhiteSpace);
    assertSame(psiFile.getChildren()[2].getNode().getElementType(), AsciiDocTokenTypes.TEXT);
  }

  public void testListingLanguage() {
    PsiFile psiFile = configureByAsciiDoc("[source,java]\n----\nfoo\n----\n");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertEquals("source-java", listing.getFenceLanguage());
  }

  public void testDiagramLanguage() {
    PsiFile psiFile = configureByAsciiDoc("[plantuml]\n----\nfoo\n----\n");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertEquals("diagram-plantuml", listing.getFenceLanguage());
  }

  public void testDescriptionImagePlain() {
    PsiFile psiFile = configureByAsciiDoc("image::test.png[]");
    AsciiDocBlockMacro macro = PsiTreeUtil.getChildOfType(psiFile, AsciiDocBlockMacro.class);
    assertNotNull(macro);
    assertEquals("(image)", macro.getDescription());
    assertEquals("image::", macro.getFoldedSummary());
  }

  public void testDescriptionImageWithTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Title\nimage::test.adoc[]");
    AsciiDocBlockMacro macro = PsiTreeUtil.getChildOfType(psiFile, AsciiDocBlockMacro.class);
    assertNotNull(macro);
    assertEquals("Title", macro.getDescription());
    assertEquals(".Title", macro.getFoldedSummary());
  }

  public void testDescriptionListingPlain() {
    PsiFile psiFile = configureByAsciiDoc("----\nListing\n----\n");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertEquals("(Listing)", listing.getDescription());
    assertEquals("----", listing.getFoldedSummary());
  }

  public void testDescriptionListingWithAttribute() {
    PsiFile psiFile = configureByAsciiDoc("[source]\n----\nListing\n----\n");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertEquals("[source]", listing.getDescription());
    assertEquals("[source]", listing.getFoldedSummary());
  }

  public void testDescriptionListingWithAttributeAndTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Title\n[source]\n----\nListing\n----\n");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertEquals("[source] Title", listing.getDescription());
    assertEquals(".Title", listing.getFoldedSummary());
  }

  public void testDescriptionListingWithTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Title\n----\nListing\n----\n");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertEquals("Title", listing.getDescription());
    assertEquals(".Title", listing.getFoldedSummary());
  }

  public void testDescriptionSection() {
    PsiFile psiFile = configureByAsciiDoc("== Section Title");
    AsciiDocSectionImpl section = PsiTreeUtil.getChildOfType(psiFile, AsciiDocSectionImpl.class);
    assertNotNull(section);
    assertEquals("Section Title", section.getDescription());
    assertEquals("== Section Title", section.getFoldedSummary());
  }

  public void testDescriptionSectionWithId() {
    PsiFile psiFile = configureByAsciiDoc("[[id]]\n== Section Title");
    AsciiDocSectionImpl section = PsiTreeUtil.getChildOfType(psiFile, AsciiDocSectionImpl.class);
    assertNotNull(section);
    assertEquals("Section Title", section.getDescription());
    assertEquals("== Section Title", section.getFoldedSummary());
  }

  public void testLinkInlineMacro() {
    PsiFile psiFile = configureByAsciiDoc("some text link:file#anchor[Text] another text");
    AsciiDocLink link = PsiTreeUtil.getChildOfType(psiFile, AsciiDocLink.class);
    assertNotNull(link);
  }

  public void testUrlInline() {
    PsiFile psiFile = configureByAsciiDoc("some text http://www.gmx.net[Text] another text");
    AsciiDocUrl link = PsiTreeUtil.getChildOfType(psiFile, AsciiDocUrl.class);
    assertNotNull(link);
    assertNotNull(link.getReference());
  }

  public void testUndelimitedBlockEndsAtBlankLink() {
    PsiFile psiFile = configureByAsciiDoc("[example]\nhttp://www.gmx.net\n\nTest");
    AsciiDocBlock block = PsiTreeUtil.getChildOfType(psiFile, AsciiDocBlock.class);
    assertNotNull(block);
  }

  public void testReferenceForAttribute() {
    // given...
    PsiFile psiFile = configureByAsciiDoc(":myattr:\n{myattr}");

    // then...
    AsciiDocAttributeDeclaration declaration = PsiTreeUtil.getChildOfType(psiFile, AsciiDocAttributeDeclaration.class);
    AsciiDocAttributeReference reference = PsiTreeUtil.getChildOfType(psiFile, AsciiDocAttributeReference.class);
    assertNotNull("declaration should exist", declaration);
    assertEquals("declaration should have name 'myattr'", "myattr", declaration.getAttributeName());
    assertNotNull("reference should exist", reference);
    assertEquals("reference should have one reference", 1, reference.getReferences().length);
    AsciiDocAttributeDeclarationName resolved = (AsciiDocAttributeDeclarationName) reference.getReferences()[0].resolve();
    assertNotNull("reference should resolve", resolved);
    assertEquals("reference should resolve to 'myattr'", "myattr", resolved.getName());
  }

  public void testValueForAttributeValue() {
    // given...
    PsiFile psiFile = configureByAsciiDoc(":myattr: Hi \\\n ho");

    // then...
    AsciiDocAttributeDeclaration declaration = PsiTreeUtil.getChildOfType(psiFile, AsciiDocAttributeDeclaration.class);
    assertNotNull("declaration should exist", declaration);
    assertEquals("declaration should have value 'Hi ho'", "Hi ho", declaration.getAttributeValue());
  }

  public void testUnsetForAttribute() {
    // given...
    PsiFile psiFile = configureByAsciiDoc(":myattr!:");

    // then...
    AsciiDocAttributeDeclarationImpl declaration = PsiTreeUtil.getChildOfType(psiFile, AsciiDocAttributeDeclarationImpl.class);
    assertNotNull("declaration exists", declaration);
    assertTrue("declaration is of unset type", declaration.isUnset());
  }

  public void testNestedReferenceInInclude() {
    // given...
    PsiFile psiFile = configureByAsciiDoc(":file: aaa.adoc\n:val: {file}\ninclude::{val}[]");

    // then...
    AsciiDocBlockMacro macro = PsiTreeUtil.getChildOfType(psiFile, AsciiDocBlockMacro.class);
    assertNotNull("macro should exist", macro);
    assertEquals("macro should have one reference", 1, macro.getReferences().length);
    AsciiDocFile resolved = (AsciiDocFile) macro.getReferences()[0].resolve();
    assertNotNull("macro should resolve", resolved);
    assertEquals("macro should resolve to file 'aaa.adoc'", "aaa.adoc", resolved.getName());
  }

  public void testIncludeWithPreBlock() {
    // given...
    PsiFile psiFile = configureByAsciiDoc("[[id]]\ninclude::aaa.adoc[]");

    // then...
    AsciiDocBlockMacro macro = PsiTreeUtil.getChildOfType(psiFile, AsciiDocBlockMacro.class);
    assertNotNull("macro should exist", macro);
    assertEquals("macro should have one reference", 1, macro.getReferences().length);
    AsciiDocFile resolved = (AsciiDocFile) macro.getReferences()[0].resolve();
    assertNotNull("macro should resolve", resolved);
    assertEquals("macro should resolve to file 'aaa.adoc'", "aaa.adoc", resolved.getName());
  }

  public void testFrontmatter() {
    // given...
    PsiFile psiFile = configureByAsciiDoc("---\nhi: ho\n---");

    // then...
    AsciiDocFrontmatter frontmatter = PsiTreeUtil.getChildOfType(psiFile, AsciiDocFrontmatter.class);
    assertNotNull("frontmatter should exist", frontmatter);
  }

  public void testIncludeWithTag() {
    // given...
    PsiFile psiFile = configureByAsciiDoc("include::aaa.adoc[tags=hi]");

    // then...
    Collection<AsciiDocIncludeTagInDocument> includeTags = PsiTreeUtil.findChildrenOfType(psiFile, AsciiDocIncludeTagInDocument.class);
    assertEquals("macro should have one include tag", 1, includeTags.size());
  }

  public void testAttributeInBlockMacroInListing() {
    // given...
    PsiFile psiFile = configureByAsciiDoc(":file: aaa.adoc\n----\ninclude::{file}[]\n----\n");

    // then...
    AsciiDocBlockMacro macro = PsiTreeUtil.getChildOfType(PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class), AsciiDocBlockMacro.class);
    assertNotNull("macro should exist", macro);
    assertEquals("macro should have one reference", 1, macro.getReferences().length);
    AsciiDocFile resolved = (AsciiDocFile) macro.getReferences()[0].resolve();
    assertNotNull("macro should resolve", resolved);
    assertEquals("macro should resolve to file 'aaa.adoc'", "aaa.adoc", resolved.getName());
  }

  public void testNestedAttribute() {
    // given...
    PsiFile psiFile = configureByAsciiDoc(":myattr: {otherattr}val");

    // then...
    AsciiDocAttributeDeclaration declaration = PsiTreeUtil.getChildOfType(psiFile, AsciiDocAttributeDeclaration.class);
    AsciiDocAttributeReference reference = PsiTreeUtil.getChildOfType(declaration, AsciiDocAttributeReference.class);
    assertNotNull("declaration should exist", declaration);
    assertNotNull("reference should exist", reference);
  }

  @Override
  protected String getTestDataPath() {
    return new File("testData/" + getBasePath()).getAbsolutePath() + "/psi/";
  }

  public void testGradleSnippets() {
    // given...
    PsiFile[] psiFile = myFixture.configureByFiles(
      getTestName(true) + "/src/docs/asciidoc/apidocs.adoc",
      getTestName(true) + "/build.gradle",
      getTestName(true) + "/build/generated-snippets/example/curl-request.adoc"
    );
    AsciiDocBlockMacro[] macros = PsiTreeUtil.getChildrenOfType(psiFile[0], AsciiDocBlockMacro.class);
    Objects.requireNonNull(macros);
    assertSize(2, macros);

    PsiReference[] referencesOperation = macros[0].getReferences();
    assertSize(1, referencesOperation);
    // finish test here. Reference will not resolve in the test, files are "temp://" files

    PsiReference[] referencesInclude = macros[1].getReferences();
    assertSize(3, referencesInclude);
    // finish test here. Reference will not resolve in the test, files are "temp://" files
  }

  public void testGradleKtsSnippets() {
    // given...
    PsiFile[] psiFile = myFixture.configureByFiles(
      getTestName(true) + "/src/docs/asciidoc/apidocs.adoc",
      getTestName(true) + "/build.gradle.kts",
      getTestName(true) + "/build/generated-snippets/example/curl-request.adoc"
    );
    AsciiDocBlockMacro[] macros = PsiTreeUtil.getChildrenOfType(psiFile[0], AsciiDocBlockMacro.class);
    Objects.requireNonNull(macros);
    assertSize(2, macros);

    PsiReference[] referencesOperation = macros[0].getReferences();
    assertSize(1, referencesOperation);
    // finish test here. Reference will not resolve in the test, files are "temp://" files

    PsiReference[] referencesInclude = macros[1].getReferences();
    assertSize(3, referencesInclude);
    // finish test here. Reference will not resolve in the test, files are "temp://" files
  }

  public void testMavenSnippets() {
    // given...
    PsiFile[] psiFile = myFixture.configureByFiles(
      getTestName(true) + "/src/docs/asciidoc/apidocs.adoc",
      getTestName(true) + "/pom.xml",
      getTestName(true) + "/target/generated-snippets/example/curl-request.adoc"
    );
    AsciiDocBlockMacro[] macros = PsiTreeUtil.getChildrenOfType(psiFile[0], AsciiDocBlockMacro.class);
    Objects.requireNonNull(macros);
    assertSize(2, macros);

    PsiReference[] referencesOperation = macros[0].getReferences();
    assertSize(1, referencesOperation);
    // finish test here. Reference will not resolve in the test, files are "temp://" files

    PsiReference[] referencesInclude = macros[1].getReferences();
    assertSize(3, referencesInclude);
    // finish test here. Reference will not resolve in the test, files are "temp://" files
  }

  public void testAntoraModule() {
    // given...
    PsiFile[] psiFile = myFixture.configureByFiles(
      getTestName(true) + "/modules/ROOT/pages/test.adoc",
      getTestName(true) + "/modules/ROOT/attachments/attachment.txt",
      getTestName(true) + "/modules/ROOT/examples/example.txt",
      getTestName(true) + "/modules/ROOT/images/image.txt",
      getTestName(true) + "/modules/ROOT/partials/part.adoc",
      getTestName(true) + "/antora.yml"
    );

    List<AttributeDeclaration> attributes = AsciiDocUtil.findAttributes(psiFile[0].getProject(), psiFile[0].getFirstChild());

    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("partialsdir", "/src/antoraModule/modules/ROOT/partials")));
    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("imagesdir", "/src/antoraModule/modules/ROOT/images")));
    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("attachmentsdir", "/src/antoraModule/modules/ROOT/attachments")));
    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("examplesdir", "/src/antoraModule/modules/ROOT/examples")));
    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("myattr", "myval")));

    AsciiDocBlockMacro[] macros = PsiTreeUtil.getChildrenOfType(psiFile[0], AsciiDocBlockMacro.class);
    assertNotNull(macros);
    assertSize(4, macros);

    // image
    assertSize(1, macros[0].getReferences());
    // finish test here. Reference will not resolve in the test, files are "temp://" files

    // examples include
    assertSize(2, macros[1].getReferences());
    // finish test here. Reference will not resolve in the test, files are "temp://" files

    // partials include
    assertSize(2, macros[2].getReferences());
    // finish test here. Reference will not resolve in the test, files are "temp://" files

    // part include via module
    assertSize(3, macros[3].getReferences());
    assertEquals("my-component:ROOT", macros[3].getReferences()[0].getCanonicalText());
    assertEquals("partial", macros[3].getReferences()[1].getCanonicalText());
    assertEquals("part.adoc", macros[3].getReferences()[2].getCanonicalText());
    // finish test here. Reference will not resolve in the test, files are "temp://" files

    AsciiDocLink[] urls = PsiTreeUtil.getChildrenOfType(psiFile[0], AsciiDocLink.class);
    assertNotNull(urls);
    assertSize(2, urls);

    // link
    assertSize(2, urls[0].getReferences());
    // finish test here. Reference will not resolve in the test, files are "temp://" files

    // xref
    assertSize(2, urls[1].getReferences());
    // finish test here. Reference will not resolve in the test, files are "temp://" files

  }

  private PsiFile configureByAsciiDoc(String text) {
    return myFixture.configureByText(AsciiDocFileType.INSTANCE, text);
  }

}

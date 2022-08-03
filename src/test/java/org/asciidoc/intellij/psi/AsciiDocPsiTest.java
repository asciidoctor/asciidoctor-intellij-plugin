package org.asciidoc.intellij.psi;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.grazie.text.TextContent;
import com.intellij.grazie.text.TextExtractor;
import com.intellij.ide.plugins.PluginEnabler;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.spellchecker.inspections.Splitter;
import com.intellij.spellchecker.tokenizer.TokenConsumer;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.asciidoc.intellij.AsciiDocSpellcheckingStrategy;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.grazie.AsciiDocGrazieTextExtractor;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.assertj.core.api.Assertions;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl;
import org.jetbrains.yaml.psi.impl.YAMLQuotedTextImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Tests for {@link org.asciidoc.intellij.parser.AsciiDocParserImpl}.
 * HINT: instead of this test, consider a known good test in {@link AsciiDocParserTest}
 *
 * @author yole
 */
@SuppressWarnings({"AsciiDocLinkResolve", "AsciiDocHeadingStyle", "AsciiDocAttributeShouldBeDefined"})
public class AsciiDocPsiTest extends BasePlatformTestCase {

  static {
    Set<PluginId> disabled = new HashSet<>();
    Set<PluginId> enabled = new HashSet<>();

    // to improve performance, remove plugins used for debugging in interactive mode
    disabled.add(PluginId.getId("PsiViewer"));
    disabled.add(PluginId.getId("PlantUML integration"));
    disabled.add(PluginId.getId("com.intellij.platform.images"));
    disabled.add(PluginId.getId("com.intellij.javafx"));

    PluginEnabler.HEADLESS.disableById(disabled);
    PluginEnabler.HEADLESS.enableById(enabled);
  }

  public void testImageBlockMacro() {
    PsiFile psiFile = configureByAsciiDoc("image::foo.png[Foo]");
    AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) psiFile.getChildren()[0];
    assertEquals("image", blockMacro.getMacroName());
    PsiReference[] references = blockMacro.getReferences();
    assertEquals(1, references.length);
  }

  public void testVideoBlockMacroLocal() {
    PsiFile psiFile = configureByAsciiDoc("video::foo.mp4[]");
    AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) psiFile.getChildren()[0];
    assertEquals("video", blockMacro.getMacroName());
    PsiReference[] references = blockMacro.getReferences();
    assertEquals(1, references.length);
  }

  public void testVideoBlockMacroRemotePlain() {
    PsiFile psiFile = configureByAsciiDoc("video::foo.mp4[youtube]");
    AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) psiFile.getChildren()[0];
    assertEquals("video", blockMacro.getMacroName());
    PsiReference[] references = blockMacro.getReferences();
    assertEquals(0, references.length);
  }

  public void testVideoBlockMacroRemoteAttribute() {
    PsiFile psiFile = configureByAsciiDoc("video::foo.mp4[poster=vimeo]");
    AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) psiFile.getChildren()[0];
    assertEquals("video", blockMacro.getMacroName());
    PsiReference[] references = blockMacro.getReferences();
    assertEquals(0, references.length);
  }

  public void testBlockId() {
    PsiFile psiFile = configureByAsciiDoc("[#id]");
    AsciiDocBlockId blockMacro = PsiTreeUtil.findChildOfType(psiFile, AsciiDocBlockId.class);
    assertNotNull(blockMacro);
    assertEquals(blockMacro.getName(), "id");
  }

  public void testBlockIdWithAttributeReference() {
    PsiFile psiFile = configureByAsciiDoc("[#id{attr}]");
    AsciiDocBlockId blockMacro = PsiTreeUtil.findChildOfType(psiFile, AsciiDocBlockId.class);
    assertNotNull(blockMacro);
    assertEquals(blockMacro.getName(), "id{attr}");
  }

  public void testBlockIdInBlockAttributesWithAttributeReference() {
    PsiFile psiFile = configureByAsciiDoc("[id=\"id{attr}\"]");
    AsciiDocBlockId blockMacro = PsiTreeUtil.findChildOfType(psiFile, AsciiDocBlockId.class);
    assertNotNull(blockMacro);
    assertEquals(blockMacro.getName(), "id{attr}");
    AsciiDocBlockId newElement = WriteCommandAction.runWriteCommandAction(myFixture.getProject(),
      (Computable<AsciiDocBlockId>) () -> (AsciiDocBlockId) blockMacro.setName("newid{newattr}"));
    assertEquals(newElement.getName(), "newid{newattr}");
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
    assertEquals(3, blockMacro.getReferences()[0].getVariants().length);
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
    Assertions.assertThat(ref.getReferences()[0].getVariants().length).isGreaterThan(1);
    Assertions.assertThat(((LookupElementBuilder) ref.getReferences()[0].getVariants()[0]).getLookupString()).isEqualTo("hi");
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

    assertEquals(AsciiDocElementTypes.LIST, children[0].getNode().getElementType());

    assertEquals("\n", children[1].getText());

    assertEquals("\n", children[2].getText());

    assertEquals("Text", children[3].getText());
    assertEquals(AsciiDocElementTypes.BLOCK, children[3].getNode().getElementType());
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
    assertEquals(3, blocks.length);
  }

  public void testListingWithBlankAfterDelimiter() {
    PsiFile psiFile = configureByAsciiDoc("[source]\n---- \nfoo\n----\nText");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertTrue(psiFile.getChildren()[1] instanceof PsiWhiteSpace);
    assertSame(psiFile.getChildren()[2].getNode().getElementType(), AsciiDocElementTypes.BLOCK);
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
    assertNull(macro.getTitle());
    assertEquals("image::test.png[]", macro.getFoldedSummary());
  }

  public void testDescriptionNestedNote() {
    PsiFile psiFile = configureByAsciiDoc("[#id]\n" +
      "Test:: Value\n" +
      "+\n" +
      "[NOTE]\n" +
      "====\n" +
      "Note\n" +
      "====\n");
    AsciiDocDescriptionItem descriptionItem = PsiTreeUtil.getChildOfType(PsiTreeUtil.getChildOfType(psiFile, AsciiDocList.class), AsciiDocDescriptionItem.class);
    assertNotNull(descriptionItem);
    assertEquals("Test: Value", descriptionItem.getFoldedSummary());
    AsciiDocBlock block = PsiTreeUtil.getChildOfType(descriptionItem, AsciiDocBlock.class);
    assertNotNull(block);
    assertEquals("[NOTE] Note", block.getFoldedSummary());
  }

  public void testDescriptionImageWithTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Title\nimage::test.adoc[]");
    AsciiDocBlockMacro macro = PsiTreeUtil.getChildOfType(psiFile, AsciiDocBlockMacro.class);
    assertNotNull(macro);
    assertEquals("Title", macro.getTitle());
    assertEquals("Title", macro.getFoldedSummary());
  }

  public void testDescriptionListingPlain() {
    PsiFile psiFile = configureByAsciiDoc("----\nListing\n----\n");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertNull(listing.getTitle());
    assertEquals("Listing", listing.getDefaultTitle());
    assertEquals("----", listing.getFoldedSummary());
  }

  public void testDescriptionListingWithAttribute() {
    PsiFile psiFile = configureByAsciiDoc("[source]\n----\nListing\n----\n");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertNull(listing.getTitle());
    assertEquals("Listing", listing.getDefaultTitle());
    assertEquals("[source]", listing.getFoldedSummary());
  }

  public void testDescriptionListingWithAttributeAndTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Title\n[source]\n----\nListing\n----\n");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertEquals("Title", listing.getTitle());
    assertEquals(".Title", listing.getFoldedSummary());
  }

  public void testDescriptionListingWithTitle() {
    PsiFile psiFile = configureByAsciiDoc(".Title\n----\nListing\n----\n");
    AsciiDocListing listing = PsiTreeUtil.getChildOfType(psiFile, AsciiDocListing.class);
    assertNotNull(listing);
    assertEquals("Title", listing.getTitle());
    assertEquals(".Title", listing.getFoldedSummary());
  }

  public void testDescriptionSection() {
    PsiFile psiFile = configureByAsciiDoc("== Section Title");
    AsciiDocSectionImpl section = PsiTreeUtil.getChildOfType(psiFile, AsciiDocSectionImpl.class);
    assertNotNull(section);
    assertEquals("Section Title", section.getTitle());
    assertEquals("== Section Title", section.getFoldedSummary());
  }

  public void testDescriptionSectionWithId() {
    PsiFile psiFile = configureByAsciiDoc("[[id]]\n== Section Title");
    AsciiDocSectionImpl section = PsiTreeUtil.getChildOfType(psiFile, AsciiDocSectionImpl.class);
    assertNotNull(section);
    assertEquals("Section Title", section.getTitle());
    assertEquals("== Section Title", section.getFoldedSummary());
    assertNotNull(section.getBlockId());
  }

  public void testDescriptionSectionWithIdAtEndOfLine() {
    PsiFile psiFile = configureByAsciiDoc("== Section Title [[id]]");
    AsciiDocSectionImpl section = PsiTreeUtil.getChildOfType(psiFile, AsciiDocSectionImpl.class);
    assertNotNull(section);
    assertEquals("Section Title", section.getTitle());
    assertEquals("== Section Title [[id]]", section.getFoldedSummary());
    assertNotNull(section.getBlockId());
  }

  public void testLinkInlineMacro() {
    PsiFile psiFile = configureByAsciiDoc("some text link:file#anchor[Text] another text");
    AsciiDocLink link = PsiTreeUtil.getChildOfType(PsiTreeUtil.getChildOfType(psiFile, AsciiDocBlock.class), AsciiDocLink.class);
    assertNotNull(link);
  }

  public void testUrlInline() {
    PsiFile psiFile = configureByAsciiDoc("some text http://www.gmx.net[Text] another text");
    AsciiDocUrl link = PsiTreeUtil.getChildOfType(PsiTreeUtil.getChildOfType(psiFile, AsciiDocBlock.class), AsciiDocUrl.class);
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
    AsciiDocAttributeReference reference = PsiTreeUtil.getChildOfType(PsiTreeUtil.getChildOfType(psiFile, AsciiDocBlock.class), AsciiDocAttributeReference.class);
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

  public void testListSeparatedByComment() {
    // given...
    PsiFile psiFile = configureByAsciiDoc("* list 1\n" +
      "* another item\n" +
      "\n" +
      "// break\n" +
      "\n" +
      ". not a nested item\n");

    // then...
    AsciiDocList[] lists = PsiTreeUtil.getChildrenOfType(psiFile, AsciiDocList.class);
    Assertions.assertThat(lists).describedAs("two separated lists").hasSize(2);
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

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void testSpellCheck() {
    // given...
    PsiFile psiFile = configureByAsciiDoc("// not within block, therefore separate elements\n" +
      "**A**nother\n\n" +
      "// within block, therefore split by word breaks\n" +
      "== Heading\n**E**quivalent **M**odulo\n" +
      "TEXTfootnote:[This is a footnote]\n" +
      "kbd:[Keyboard]\n" +
      "btn:[Button]\n" +
      "A <<id,reftext>>.\n" +
      "|===\n" +
      "| Cell contents.\n" +
      "|===");

    AsciiDocSpellcheckingStrategy spellcheckingStrategy = new AsciiDocSpellcheckingStrategy();

    List<String> tokens = new ArrayList<>();
    TokenConsumer tk = new TokenConsumer() {
      @Override
      public void consumeToken(PsiElement element, String text, boolean useRename, int offset, TextRange rangeToCheck, Splitter splitter) {
        tokens.add(text);
      }
    };
    psiFile.accept(new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        Tokenizer tokenizer = spellcheckingStrategy.getTokenizer(element);
        tokenizer.tokenize(element, tk);
        element.acceptChildren(this);
      }
    });
    // for now headings prefix and comment prefix remain in the text, as the spell checker will ignore them anyway
    Assertions.assertThat(tokens).containsExactlyInAnyOrder("// not within block, therefore separate elements",
      "Another",
      "// within block, therefore split by word breaks",
      "Equivalent",
      "Modulo",
      "TEXT",
      "Button",
      "A",
      "reftext.",
      "== Heading",
      "This",
      "is",
      "a",
      "footnote",
      "Cell",
      "contents.");
  }

  public void testGrammarCheck() {
    PsiFile psiFile = configureByAsciiDoc("// comment with some text\n" +
      "== A heading\n" +
      ":description: attribute \\\ncontinuation\n" +
      "Textfootnote:[A footnote.] with a footnote.\n" +
      "A '`test`' test`'s\n" +
      "\n" +
      "An old heading\n" +
      "+++++++++++++\n" +
      "A <<id,reftext>>.\n" +
      "A xref:file.adoc[link].\n" +
      "An email@example.com.\n" +
      "An mailto:email@address.com[email with text].\n" +
      "A http://example.com URL.\n" +
      "A http://example.com[URL with text].\n" +
      "A link:http://example.com[URL with text as link].\n" +
      "\n" +
      "Definition:: Text" +
      "\n" +
      "* List item" +
      "\n" +
      "Something \"`quoted`\"\n" +
      "|===\n" +
      "| Cell Content.\n" +
      "|===");

    Set<String> texts = new HashSet<>();
    Set<TextContent.TextDomain> textContents = new HashSet<>();
    textContents.add(TextContent.TextDomain.COMMENTS);
    textContents.add(TextContent.TextDomain.DOCUMENTATION);
    textContents.add(TextContent.TextDomain.PLAIN_TEXT);

    PsiElementVisitor myVisitor = new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        List<TextContent> textAt = TextExtractor.findTextsAt(element, textContents);
        for (TextContent textContent : textAt) {
          String text = textContent.toString().trim();
          for (String line : text.split("\\n", -1)) {
            if (line.length() > 0) {
              texts.add(line);
            }
          }
        }
        PsiElement child = element.getFirstChild();
        while (child != null) {
          visitElement(child);
          child = child.getNextSibling();
        }
      }
    };
    psiFile.accept(myVisitor);
    Assertions.assertThat(texts).containsExactlyInAnyOrder(
      "A heading",
      "attribute continuation",
      "Cell Content.",
      "Text with a footnote.",
      "A 'test' test's",
      "A reftext.",
      "An old heading",
      "comment with some text",
      "A footnote.",
      "A link.",
      "Definition: Text",
      "List item",
      "A URL with text as link.",
      "A URL with text.",
      "An email@example.com.",
      "An email with text.",
      "A http://example.com URL.",
      "Something quoted"
    );

  }

  public void testGrammarStringStripBlanks() {
    // given...
    PsiFile psiFile = configureByAsciiDoc("== Heading\n\nthis  test\n\n");
    AsciiDocSection section = PsiTreeUtil.getChildOfType(psiFile, AsciiDocSection.class);
    AsciiDocBlock block = PsiTreeUtil.getChildOfType(section, AsciiDocBlock.class);
    Assertions.assertThat(block).isNotNull();

    // when...
    TextContent sectionResult = new AsciiDocGrazieTextExtractor().buildTextContent(block, Set.of(TextContent.TextDomain.PLAIN_TEXT));
    TextContent blockResult = new AsciiDocGrazieTextExtractor().buildTextContent(block, Set.of(TextContent.TextDomain.PLAIN_TEXT));

    // then...
    // ... IntelliJ 2021.2 will trim beginning and end blanks, and the grammar part should trim the spaces
    Assertions.assertThat(sectionResult).isNotNull();
    Assertions.assertThat(blockResult).isNotNull();
    Assertions.assertThat(blockResult.toString()).isEqualTo("this test");
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

  public void testDesc() {
    // given...
    PsiFile psiFile = configureByAsciiDoc(":myattr: {otherattr}val");

    // then...
    AsciiDocAttributeDeclaration declaration = PsiTreeUtil.getChildOfType(psiFile, AsciiDocAttributeDeclaration.class);
    AsciiDocAttributeReference reference = PsiTreeUtil.getChildOfType(declaration, AsciiDocAttributeReference.class);
    assertNotNull("declaration should exist", declaration);
    assertNotNull("reference should exist", reference);
  }

  public void testXrefResolution() {
    // given...
    @SuppressWarnings({"AsciiDocAnchorWithoutId", "AsciiDocXrefWithNaturalCrossReference"})
    PsiFile psiFile = configureByAsciiDoc("xref:A Section[]\n<<A Section>>\nxref:_a_section[]\n\n== A Section\n");

    // then...
    AsciiDocLink[] links = PsiTreeUtil.getChildrenOfType(PsiTreeUtil.getChildOfType(psiFile, AsciiDocBlock.class), AsciiDocLink.class);
    assertNotNull(links);
    for (AsciiDocLink link : links) {
      PsiReference[] references = link.getReferences();
      Assertions.assertThat(references).hasSize(1);
      PsiElement declaration = references[0].resolve();
      assertNotNull("declaration should exist", declaration);
    }
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

    assertReferencesResolve(macros[0], 1);
    assertReferencesResolve(macros[1], 3);
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

    assertReferencesResolve(macros[0], 1);
    assertReferencesResolve(macros[1], 3);
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
    assertReferencesResolve(macros[0], 1);
    assertReferencesResolve(macros[1], 3);
  }

  public void testAntoraModule() {
    // given...
    PsiFile[] psiFile = myFixture.configureByFiles(
      getTestName(true) + "/componentV1/modules/ROOT/pages/test.adoc",
      getTestName(true) + "/componentV1/modules/ROOT/pages/page-in-root.adoc",
      getTestName(true) + "/componentV1/modules/ROOT/pages/sub/page-in-sub.adoc",
      getTestName(true) + "/componentV1/modules/ROOT/attachments/attachment.txt",
      getTestName(true) + "/componentV1/modules/ROOT/attachments/attachment-in-root.txt",
      getTestName(true) + "/componentV1/modules/ROOT/examples/example.txt",
      getTestName(true) + "/componentV1/modules/ROOT/images/image.txt",
      getTestName(true) + "/componentV1/modules/ROOT/images/image-in-root.txt",
      getTestName(true) + "/componentV1/modules/ROOT/partials/part.adoc",
      getTestName(true) + "/componentV1/modules/module/pages/page.adoc",
      getTestName(true) + "/componentV1/modules/module/partials/partial.adoc",
      getTestName(true) + "/componentV1/modules/ROOT/nav.adoc",
      getTestName(true) + "/componentV1/antora.yml",
      getTestName(true) + "/componentV2/modules/ROOT/pages/test.adoc",
      getTestName(true) + "/componentV2/modules/module/pages/test.adoc",
      getTestName(true) + "/componentV2/antora.yml",
      getTestName(true) + "/componentVnull/modules/ROOT/pages/test.adoc",
      getTestName(true) + "/componentVnull/antora.yml",
      getTestName(true) + "/antora-playbook.yml"
    );

    List<AttributeDeclaration> attributes = AsciiDocUtil.findAttributes(psiFile[0].getProject(), psiFile[0].getFirstChild());

    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("partialsdir", "/src/antoraModule/componentV1/modules/ROOT/partials")));
    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("imagesdir", "/src/antoraModule/componentV1/modules/ROOT/images")));
    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("attachmentsdir", "/src/antoraModule/componentV1/modules/ROOT/attachments")));
    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("examplesdir", "/src/antoraModule/componentV1/modules/ROOT/examples")));
    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("myattr", "myval")));
    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("pbkey", "pbval")));
    assertTrue(attributes.contains(new AsciiDocAttributeDeclarationDummy("pbsoftkey", "pbsoftval@")));

    AsciiDocBlockMacro[] macros = PsiTreeUtil.getChildrenOfType(psiFile[0], AsciiDocBlockMacro.class);
    assertNotNull(macros);
    assertSize(5, macros);

    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "1.0@my-component:ROOT:test.adoc", "page"), "/src/antoraModule/componentV1/modules/ROOT/pages/test.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "my-component:ROOT:test.adoc", "page"), "/src/antoraModule/componentVnull/modules/ROOT/pages/test.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "my-component::test.adoc", "page"), "/src/antoraModule/componentVnull/modules/ROOT/pages/test.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "ROOT:test.adoc", "page"), "/src/antoraModule/componentV1/modules/ROOT/pages/test.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "ROOT:page$test.adoc", null), "/src/antoraModule/componentV1/modules/ROOT/pages/test.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "page$test.adoc", null), "/src/antoraModule/componentV1/modules/ROOT/pages/test.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "test.adoc", "page"), "/src/antoraModule/componentV1/modules/ROOT/pages/test.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "1.0@test.adoc", "page"), "/src/antoraModule/componentV1/modules/ROOT/pages/test.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "2.0@test.adoc", "page"), "/src/antoraModule/componentV2/modules/ROOT/pages/test.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "_@test.adoc", "page"), "/src/antoraModule/componentVnull/modules/ROOT/pages/test.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(macros[0], "2.0@my-component:module:test.adoc", "page"), "/src/antoraModule/componentV2/modules/module/pages/test.adoc");

    // image
    assertReferencesResolve(macros[0], 1);

    // examples include
    assertReferencesResolve(macros[1], 2);

    // partials include
    assertReferencesResolve(macros[2], 2);

    // part include via module
    assertReferencesResolve(macros[3], 3);
    assertEquals("1.0@my-component:ROOT", macros[3].getReferences()[0].getCanonicalText());
    assertEquals("partial", macros[3].getReferences()[1].getCanonicalText());
    assertEquals("part.adoc", macros[3].getReferences()[2].getCanonicalText());

    ArrayList<AsciiDocLink> urls = new ArrayList<>();
    for (AsciiDocBlock block : Objects.requireNonNull(PsiTreeUtil.getChildrenOfType(psiFile[0], AsciiDocBlock.class))) {
      AsciiDocLink[] links = PsiTreeUtil.getChildrenOfType(block, AsciiDocLink.class);
      if (links != null) {
        urls.addAll(Arrays.asList(links));
      }
    }

    assertSize(10, urls);

    // link
    assertReferencesResolve(urls.get(0), 2);

    // xref to attachment
    assertReferencesResolve(urls.get(1), 2);

    // xref to page in other module
    assertReferencesResolve(urls.get(2), 2);

    // xref to old page name
    assertReferencesResolve(urls.get(3), 1);

    // xref with attribute
    assertReferencesResolve(urls.get(5), 1);

    // xref page in sub
    assertReferencesResolve(urls.get(8), 2);

    // xref page relative
    assertReferencesResolve(urls.get(9), 4);

    urls.clear();

    for (AsciiDocBlock block : Objects.requireNonNull(PsiTreeUtil.getChildrenOfType(psiFile[10], AsciiDocBlock.class))) {
      AsciiDocLink[] links = PsiTreeUtil.getChildrenOfType(block, AsciiDocLink.class);
      if (links != null) {
        urls.addAll(Arrays.asList(links));
      }
    }

    assertSize(6, urls);

    // xref to page
    assertReferencesResolve(urls.get(0), 1);

    // xref to module
    assertReferencesResolve(urls.get(1), 2);

    // xref to component and module
    assertReferencesResolve(urls.get(2), 2);

    // xref to attachment
    assertReferencesResolve(urls.get(3), 2);

    // xref to component module and family
    assertReferencesResolve(urls.get(4), 3);

    // xref to component module in sub folder
    assertReferencesResolve(urls.get(5), 3);

    urls.clear();

    for (AsciiDocList list : Objects.requireNonNull(PsiTreeUtil.getChildrenOfType(psiFile[11], AsciiDocList.class))) {
      for (AsciiDocListItem listItem : Objects.requireNonNull(PsiTreeUtil.getChildrenOfType(list, AsciiDocListItem.class))) {
        AsciiDocLink[] links = PsiTreeUtil.getChildrenOfType(listItem, AsciiDocLink.class);
        if (links != null) {
          urls.addAll(Arrays.asList(links));
        }
      }
    }

    assertSize(3, urls);

    // xref to page
    assertReferencesResolve(urls.get(0), 1);

    // xref to module
    assertReferencesResolve(urls.get(1), 2);

    // xref in sub
    assertReferencesResolve(urls.get(1), 2);

  }

  public void testAntoraRelativeResources() {
    // given...
    PsiFile[] psiFile = myFixture.configureByFiles(
      getTestName(true) + "/modules/ROOT/pages/sub/test.adoc",
      getTestName(true) + "/modules/ROOT/pages/sub/page.adoc",
      getTestName(true) + "/modules/ROOT/attachments/sub/attachment.txt",
      getTestName(true) + "/antora.yml"
    );

    AsciiDocLink[] links = PsiTreeUtil.getChildrenOfType(psiFile[0].getFirstChild(), AsciiDocLink.class);
    assertNotNull(links);
    assertSize(3, links);

    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(links[0], "./page.adoc", "page"), "/src/antoraRelativeResources/modules/ROOT/pages/sub/page.adoc");
    assertSingleListEntry(AsciiDocUtil.replaceAntoraPrefix(links[0], "my-component::./page.adoc", "page"), "/src/antoraRelativeResources/modules/ROOT/pages/sub/page.adoc");

    assertReferencesResolve(links[0], 3);
    assertReferencesResolve(links[1], 2);
    assertReferencesResolve(links[2], 3);
  }


  @SuppressWarnings("ConstantConditions")
  public void testAntoraComponentResolve() {
    // given...
    PsiFile[] psiFile = myFixture.configureByFiles(
      getTestName(true) + "/antora.yml",
      getTestName(true) + "/modules/ROOT/pages/index.adoc",
      getTestName(true) + "/modules/ROOT/nav.adoc"
    );


    YAMLSequence nav = (YAMLSequence) ((YAMLBlockMappingImpl) psiFile[0].getChildren()[0].getChildren()[0]).getKeyValueByKey("nav").getValue();
    assertReferencesResolve(nav.getItems().get(0).getValue(), 3);

    YAMLQuotedTextImpl startPage = (YAMLQuotedTextImpl) ((YAMLBlockMappingImpl) psiFile[0].getChildren()[0].getChildren()[0]).getKeyValueByKey("start_page").getValue();

    PsiReference startPageReference = startPage.getReferences()[1];
    assertNotNull("reference didn't resolve: '" + startPageReference.getRangeInElement().substring(startPage.getText()) + "' in '" + startPage.getText() + "'", startPageReference.resolve());
  }

  private void assertReferencesResolve(PsiElement element, int numberOfReferences) {
    assertSize(numberOfReferences, element.getReferences());
    for (PsiReference reference : element.getReferences()) {
      if (reference instanceof PsiPolyVariantReference) {
        assertTrue("reference didn't resolve: '" + reference.getRangeInElement().substring(element.getText()) + "' in '" + element.getText() + "'", ((PsiPolyVariantReference) reference).multiResolve(false).length > 0);
      } else {
        assertNotNull("reference didn't resolve: '" + reference.getRangeInElement().substring(element.getText()) + "' in '" + element.getText() + "'", reference.resolve());
      }
    }
  }

  @SuppressWarnings("checkstyle:AvoidNestedBlocks")
  public void testAntoraImageXref() {
    // given...
    PsiFile[] psiFile = myFixture.configureByFiles(
      getTestName(true) + "/modules/ROOT/pages/index.adoc",
      getTestName(true) + "/modules/ROOT/images/image.txt",
      getTestName(true) + "/antora.yml"
    );

    AsciiDocBlockMacro[] macros = PsiTreeUtil.getChildrenOfType(psiFile[0].getChildren()[0], AsciiDocBlockMacro.class);
    assertNotNull(macros);
    assertSize(3, macros);

    {
      AsciiDocAttributeInBrackets[] attributes = PsiTreeUtil.getChildrenOfType(macros[0], AsciiDocAttributeInBrackets.class);
      assertNotNull(attributes);
      assertSize(1, attributes);

      assertSize(1, attributes[0].getReferences());
      assertInstanceOf(attributes[0].getReferences()[0].resolve(), AsciiDocBlockIdImpl.class);
    }

    {
      AsciiDocAttributeInBrackets[] attributes = PsiTreeUtil.getChildrenOfType(macros[1], AsciiDocAttributeInBrackets.class);
      assertNotNull(attributes);
      assertSize(1, attributes);

      assertSize(2, attributes[0].getReferences());
      assertInstanceOf(attributes[0].getReferences()[0].resolve(), PsiDirectory.class);
      assertInstanceOf(attributes[0].getReferences()[1].resolve(), PsiFile.class);
    }

    {
      assertSize(1, macros[2].getReferences());
      assertInstanceOf(macros[2].getReferences()[0].resolve(), PsiFile.class);
    }

  }

  private void assertSingleListEntry(List<String> list, String entry) {
    assertSize(1, list);
    assertEquals(entry, list.get(0));
  }

  private PsiFile configureByAsciiDoc(@Language("asciidoc") String text) {
    return myFixture.configureByText(AsciiDocFileType.INSTANCE, text);
  }

}

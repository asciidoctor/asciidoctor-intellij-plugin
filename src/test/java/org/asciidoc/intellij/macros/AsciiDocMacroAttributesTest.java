package org.asciidoc.intellij.macros;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.AsciiDocDocumentationProvider;
import org.asciidoc.intellij.inspections.AsciiDocCodeInsightFixtureTestCase;

import java.util.Arrays;
import java.util.List;

public class AsciiDocMacroAttributesTest extends AsciiDocCodeInsightFixtureTestCase {

  private static final String INTENTION = "Convert positional attribute to named attribute";
  private static final String TO_POSITIONAL = "Convert named attribute to positional attribute";

  private List<String> complete(String text) {
    myFixture.configureByText("test.adoc", text);
    myFixture.completeBasic();
    List<String> strings = myFixture.getLookupElementStrings();
    return strings == null ? List.of() : strings;
  }

  public void testCompletionInBlockImageSuggestsUnusedNamedAttributes() {
    List<String> result = complete("image::a.png[Alt,role=x,<caret>]");
    assertContainsElements(result, "title", "link", "pdfwidth");
    assertDoesntContain(result, "role", "alt");
  }

  public void testCompletionInEmptyBrackets() {
    assertContainsElements(complete("image::a.png[<caret>]"), "alt", "width");
  }

  public void testCompletionInInlineImage() {
    List<String> result = complete("text image:a.png[<caret>] text");
    assertContainsElements(result, "alt", "width");
    // block only
    assertDoesntContain(result, "align", "caption");
  }

  public void testCompletionInsertsAssignment() {
    myFixture.configureByText("test.adoc", "image::a.png[pdfw<caret>]");
    myFixture.completeBasic();
    myFixture.checkResult("image::a.png[pdfwidth=<caret>]");
  }

  public void testCompletionOfValues() {
    assertContainsElements(complete("image::a.png[align=<caret>]"), "left", "center", "right");
    assertContainsElements(complete("image::a.png[Alt,loading=<caret>]"), "lazy", "eager");
    assertContainsElements(complete("image::a.png[role=x,float=<caret>]"), "left", "right");
  }

  public void testCompletionOfValueForPositionalAttribute() {
    assertContainsElements(complete("video::a[<caret>]"), "vimeo", "youtube");
    assertContainsElements(complete("icon:heart[<caret>]"), "2x", "lg");
  }

  public void testIntentionConvertsPositionalAttribute() {
    myFixture.configureByText("test.adoc", "image::a.png[Sunset,3<caret>00]");
    applySingleQuickFix(INTENTION);
    myFixture.checkResult("image::a.png[Sunset,width=300]");
  }

  public void testIntentionWithQuotedValueContainingComma() {
    myFixture.configureByText("test.adoc", "image::a.png[\"a,b\",3<caret>00]");
    applySingleQuickFix(INTENTION);
    myFixture.checkResult("image::a.png[\"a,b\",width=300]");
  }

  public void testIntentionNamedToPositionalReordersAttributes() {
    myFixture.configureByText("test.adoc", "image::a.png[role=x,wid<caret>th=300]");
    applySingleQuickFix(TO_POSITIONAL);
    myFixture.checkResult("image::a.png[,300,role=x]");
  }

  public void testIntentionNamedToPositionalKeepsOtherPositionalAttributes() {
    myFixture.configureByText("test.adoc", "image::a.png[Sunset,,hei<caret>ght=\"1, 2\"]");
    applySingleQuickFix(TO_POSITIONAL);
    myFixture.checkResult("image::a.png[Sunset,,\"1, 2\"]");
  }

  public void testIntentionNamedToPositionalAfterQuotedPositionalAttribute() {
    myFixture.configureByText("test.adoc", "image::a.png[\"xx\",wid<caret>th=300,role=x]");
    applySingleQuickFix(TO_POSITIONAL);
    myFixture.checkResult("image::a.png[\"xx\",300,role=x]");
  }

  public void testIntentionPositionalToNamedForQuotedAttribute() {
    myFixture.configureByText("test.adoc", "image:a.png['x, y',3<caret>00]");
    applySingleQuickFix(INTENTION);
    myFixture.checkResult("image:a.png['x, y',width=300]");
  }

  public void testIntentionNamedToPositionalInline() {
    myFixture.configureByText("test.adoc", "image:a.png[title=T,al<caret>t=Sunset]");
    applySingleQuickFix(TO_POSITIONAL);
    myFixture.checkResult("image:a.png[Sunset,title=T]");
  }

  public void testIntentionNamedToPositionalNotAvailable() {
    // not a positional attribute of the macro
    assertNoIntention("image::a.png[ro<caret>le=x]", TO_POSITIONAL);
    // position is used by another attribute
    assertNoIntention("image::a.png[Sunset,200,wid<caret>th=3]", TO_POSITIONAL);
    // already positional
    assertNoIntention("image::a.png[Sun<caret>set]", TO_POSITIONAL);
  }

  public void testIntentionWithEmptySlotAndNamedBefore() {
    myFixture.configureByText("test.adoc", "image::a.png[role=x,,<caret>200]");
    applySingleQuickFix(INTENTION);
    myFixture.checkResult("image::a.png[role=x,,height=200]");
  }

  public void testIntentionNotAvailable() {
    // already named
    assertNoIntention("image::a.png[wid<caret>th=3]");
    // no positional attribute at this position
    assertNoIntention("image::a.png[a,b,c,d<caret>]");
    // name is already used
    assertNoIntention("image::a.png[a<caret>,alt=b]");
    // unknown macro
    assertNoIntention("unknown::a.png[a<caret>]");
  }

  public void testDocumentationForPositionalAndNamedAttribute() {
    assertDoc("image::a.png[Alt,2<caret>00]", "<b>width</b>", "positional attribute 2");
    assertDoc("image::a.png[ro<caret>le=x]", "<b>role</b>");
    assertNull(getDocElement("image::a.png[A,1,2,3<caret>]"));
    assertDoc("image::a.png[align=ce<caret>nter]", "<b>center</b>", "Centers the image");
    assertDoc("image::a.png[align=unk<caret>nown]", "<b>align</b>");
    assertDoc("video::a[vim<caret>eo]", "<b>vimeo</b>", "Vimeo");
  }

  public void testMetadataIsConsistent() {
    assertFalse(AsciiDocMacroAttributes.getMacros().isEmpty());
    for (AsciiDocMacroAttributes.Macro macro : AsciiDocMacroAttributes.getMacros()) {
      for (String positional : macro.positional()) {
        assertNotNull(macro.name() + " has an undefined positional attribute " + positional, macro.find(positional));
      }
      assertEquals(macro.name(), macro.attributes().size(),
        macro.attributes().stream().map(AsciiDocMacroAttributes.Attribute::name).distinct().count());
    }
  }

  public void testDocumentationForMacro() {
    assertDoc("ima<caret>ge::a.png[]", "<b>image::</b>", "Positional attributes:", "alt, width, height", "macros/images/");
    assertDoc("text im<caret>age:a.png[] text", "<b>image:</b>");
    assertDoc("text kb<caret>d:[Ctrl+C] text", "<b>kbd:</b>", "keyboard-macro");
    assertDoc("see li<caret>nk:a.html[text]", "<b>link:</b>", "Named attributes:");
  }

  private void assertNoIntention(String text) {
    assertNoIntention(text, INTENTION);
  }

  private void assertNoIntention(String text, String intention) {
    myFixture.configureByText("test.adoc", text);
    List<IntentionAction> actions = myFixture.filterAvailableIntentions(intention);
    assertEmpty(actions);
  }

  private PsiElement getDocElement(String text) {
    myFixture.configureByText("test.adoc", text);
    PsiElement context = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    return new AsciiDocDocumentationProvider()
      .getCustomDocumentationElement(myFixture.getEditor(), myFixture.getFile(), context, myFixture.getCaretOffset());
  }

  private void assertDoc(String text, String... expected) {
    PsiElement element = getDocElement(text);
    assertNotNull(element);
    String doc = new AsciiDocDocumentationProvider().generateDoc(element, null);
    assertNotNull(doc);
    for (String e : expected) {
      assertTrue(doc + " should contain " + e, doc.contains(e));
    }
    assertNotNull(Arrays.asList(expected));
  }

  @Override
  protected String getBasePath() {
    return "macros";
  }
}

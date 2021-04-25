package org.asciidoc.intellij.injection;

import com.intellij.codeInsight.intention.impl.QuickEditAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase;
import org.junit.Test;

import java.util.function.Consumer;

/**
 * Testing when injected language content is edited for an {@link org.asciidoc.intellij.psi.AsciiDocListing}.
 * Inspired by https://github.com/monogon-dev/intellij-cue/blob/main/src/test/java/dev/monogon/cue/lang/injection/CueMultiHostInjectorTest.java
 * Credits to jansorg for testing the editing of an injected language snippet.
 *
 * @author Alexander Schwartz (alexander.schwartz@gmx.net) 2021
 */
@SuppressWarnings("JUnitAmbiguousTestClass") // ... as base class UsefulTestCase is documented to support both
public class AsciiDocInjectionTest extends LightPlatformCodeInsightFixture4TestCase {

  public static final String JSON = "{hi: 'ho'";

  @Test
  public void shouldInsertTextToFragment() {
    // given...
    // JSON is one of the few languages supported by the fixture (for example YAML is not supported)
    String text = "[source,json]\n----\n" + JSON + "}\n----\n";
    var file = myFixture.configureByText("a.adoc", text);

    // caret must not be placed in JSON content, otherwise the whole file will be switched to JSON type by fixture
    myFixture.getEditor().getCaretModel().moveToOffset(text.indexOf(JSON) + JSON.length());

    // when...
    withInjectedContent(file, fragmentFile -> edit(fragmentFile,
      doc -> doc.insertString(JSON.length(), ",\nme: 'too'"))
    );

    // then...
    myFixture.checkResult("[source,json]\n" +
      "----\n" +
      "{hi: 'ho',\n" +
      "me: 'too'}\n" +
      "----\n");
  }

  private void withInjectedContent(PsiFile file, Consumer<PsiFile> action) {
    var quickEdit = new QuickEditAction();
    var handler = quickEdit.invokeImpl(getProject(), myFixture.getEditor(), file);
    var fragmentFile = handler.getNewFile();

    action.accept(fragmentFile);
  }

  private void edit(PsiFile file, Consumer<Document> action) {
    CommandProcessor.getInstance().executeCommand(file.getProject(), () -> ApplicationManager.getApplication().runWriteAction(() -> {
      var doc = PsiDocumentManager.getInstance(getProject()).getDocument(file);
      action.accept(doc);
    }), "Change Doc", "Change doc");
  }

}

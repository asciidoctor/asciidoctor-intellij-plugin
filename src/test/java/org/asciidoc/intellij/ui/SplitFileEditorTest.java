package org.asciidoc.intellij.ui;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.DocumentUtil;
import org.asciidoc.intellij.editor.AsciiDocSplitEditorProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SplitFileEditorTest extends BasePlatformTestCase {

  public void testGetStateDuringBulkUpdateDoesNotFail() {
    myFixture.configureByText("test.adoc", "some text");
    FileEditor editor = new AsciiDocSplitEditorProvider().createEditor(getProject(), myFixture.getFile().getVirtualFile());
    try {
      SplitFileEditor<?, ?> splitEditor = (SplitFileEditor<?, ?>) editor;
      FileEditorState[] state = new FileEditorState[1];
      DocumentUtil.executeInBulk(myFixture.getEditor().getDocument(),
        () -> state[0] = splitEditor.getState(FileEditorStateLevel.NAVIGATION));
      assertThat(state[0]).isNotNull();
    } finally {
      Disposer.dispose(editor);
    }
  }

  /**
   * Documents the platform behavior that {@link SplitFileEditor#getState} works around.
   * If this test fails, the platform no longer throws and the check for the bulk update in {@link SplitFileEditor#getState} can be removed.
   */
  public void testPlatformTextEditorStillFailsDuringBulkUpdate() {
    myFixture.configureByText("test.adoc", "some text");
    FileEditor editor = TextEditorProvider.getInstance().createEditor(getProject(), myFixture.getFile().getVirtualFile());
    try {
      assertThatThrownBy(() -> DocumentUtil.executeInBulk(myFixture.getEditor().getDocument(),
        () -> editor.getState(FileEditorStateLevel.NAVIGATION)))
        .hasMessageContaining("bulk");
    } finally {
      Disposer.dispose(editor);
    }
  }
}

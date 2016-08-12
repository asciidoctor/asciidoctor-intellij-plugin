package org.asciidoc.intellij.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider;
import com.intellij.openapi.util.Disposer;
import org.asciidoc.intellij.ui.SplitTextEditorProvider;
import org.jetbrains.annotations.NotNull;

public class AsciiDocSplitEditorProvider extends SplitTextEditorProvider {
  public AsciiDocSplitEditorProvider() {
    super(new PsiAwareTextEditorProvider(), new AsciiDocPreviewEditorProvider());
  }

  @Override
  protected FileEditor createSplitEditor(@NotNull final FileEditor firstEditor, @NotNull FileEditor secondEditor) {
    if (!(firstEditor instanceof TextEditor) || !(secondEditor instanceof AsciiDocPreviewEditor)) {
      throw new IllegalArgumentException("Main editor should be TextEditor");
    }
    return new AsciiDocSplitEditor(((TextEditor)firstEditor), ((AsciiDocPreviewEditor)secondEditor));
  }

  @Override
  public void disposeEditor(@NotNull FileEditor fileEditor) {
    // default -- needed for IntelliJ IDEA 15 compatibility
    Disposer.dispose(fileEditor);
  }

}

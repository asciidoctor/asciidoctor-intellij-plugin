package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
 * @author Erik Pragt
 */
public class MakeTitle extends AsciiDocAction {

  @Override
  public boolean displayTextInToolbar() {
    // this doesn't have an icon, therefore show text
    return true;
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    final Project project = event.getProject();
    if (project == null) {
      return;
    }
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }
    final Document document = editor.getDocument();
    Pair<Integer, Integer> linePositionOffset = getLinePositionOffset(editor);
    String selectedText = document.getText(new TextRange(linePositionOffset.first, linePositionOffset.second));

    final String updatedText = updateSelection(selectedText);

    updateDocument(project, document, editor, updatedText);
  }

  @NotNull
  protected String updateSelection(String selectedText) {
    final String updatedText;
    if (selectedText.trim().equals("") || selectedText.trim().equals("\n")) {
      updatedText = "= ";
    } else if (selectedText.startsWith("= ")) {
      updatedText = selectedText.substring(2);
    } else {
      updatedText = "= " + selectedText;
    }
    return updatedText;
  }

  private void updateDocument(final Project project, final Document document, final Editor editor, final String updatedText) {
    DocumentWriteAction.run(project, () -> {
      Pair<Integer, Integer> linePositionOffset = getLinePositionOffset(editor);
      Integer start = linePositionOffset.first;
      Integer end = linePositionOffset.second;
      document.replaceString(start, end, updatedText);
      if (updatedText.startsWith("= ") && editor.getCaretModel().getCurrentCaret().getLogicalPosition().column == 0) {
        // Move the caret just after the markup (at the start of the text) while preserving the selection
        editor.getCaretModel().moveCaretRelatively(2, 0, editor.getSelectionModel().hasSelection(), false, false);
      }
    }, "MakeTitle");
  }

  private Pair<Integer, Integer> getLinePositionOffset(final Editor editor) {
    Caret caret = editor.getCaretModel().getCurrentCaret();
    Pair<LogicalPosition, LogicalPosition> lines = EditorUtil.calcCaretLineRange(caret);
    LogicalPosition lineStart = lines.first;
    LogicalPosition nextLineStart = lines.second;
    return Pair.create(editor.logicalPositionToOffset(lineStart), editor.logicalPositionToOffset(nextLineStart));
  }
}

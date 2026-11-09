package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Moves the current table row on "Move Statement Up/Down" and "Move Line Up/Down" (by default Ctrl+Shift+Up/Down and Alt+Shift+Up/Down) when the caret is inside an AsciiDoc table.
 * Everywhere else, the original handler is used.
 */
public class AsciiDocTableMoveRowHandler extends EditorActionHandler {
  private final EditorActionHandler original;
  private final boolean up;

  public AsciiDocTableMoveRowHandler(EditorActionHandler original, boolean up) {
    this.original = original;
    this.up = up;
  }

  public static class Up extends AsciiDocTableMoveRowHandler {
    public Up(EditorActionHandler original) {
      super(original, true);
    }
  }

  public static class Down extends AsciiDocTableMoveRowHandler {
    public Down(EditorActionHandler original) {
      super(original, false);
    }
  }

  @Override
  protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    return original.isEnabled(editor, caret, dataContext);
  }

  @Override
  protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
    Project project = editor.getProject();
    if (project != null && !editor.getSelectionModel().hasSelection()) {
      PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
      var file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      AsciiDocTable table = AsciiDocTableEditAction.findTable(editor, file);
      if (table != null && (up ? table.canMoveRowUp() : table.canMoveRowDown())) {
        AsciiDocTableEditAction.perform(project, editor, up ? AsciiDocTable::canMoveRowUp : AsciiDocTable::canMoveRowDown,
          t -> t.moveRow(up), up ? "Move table row up" : "Move table row down");
        return;
      }
    }
    original.execute(editor, caret, dataContext);
  }
}

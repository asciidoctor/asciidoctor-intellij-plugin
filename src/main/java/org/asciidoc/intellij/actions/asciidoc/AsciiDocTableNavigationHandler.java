package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Moves to the next or previous table cell on Tab and Shift+Tab when the caret is inside an AsciiDoc table.
 * Tab in the last cell appends a row.
 * Everywhere else, the original handler is used.
 */
public class AsciiDocTableNavigationHandler extends EditorActionHandler {
  private final EditorActionHandler original;
  private final boolean forward;

  public AsciiDocTableNavigationHandler(EditorActionHandler original, boolean forward) {
    this.original = original;
    this.forward = forward;
  }

  public static class Next extends AsciiDocTableNavigationHandler {
    public Next(EditorActionHandler original) {
      super(original, true);
    }
  }

  public static class Previous extends AsciiDocTableNavigationHandler {
    public Previous(EditorActionHandler original) {
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
    if (project != null) {
      PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
    }
    Caret target = caret == null ? editor.getCaretModel().getCurrentCaret() : caret;
    Integer offset = findTarget(editor, target);
    if (offset != null) {
      target.removeSelection();
      target.moveToOffset(offset);
    } else if (forward && isAppendRowPossible(editor, target)) {
      AsciiDocTableEditAction.perform(editor.getProject(), editor, AsciiDocTable::canAppendRow, AsciiDocTable::appendRow,
        "Append Table Row");
    } else {
      original.execute(editor, caret, dataContext);
    }
  }

  private static boolean isAppendRowPossible(@NotNull Editor editor, @NotNull Caret caret) {
    Project project = editor.getProject();
    if (project == null || caret.hasSelection() || editor.getCaretModel().getCaretCount() > 1) {
      return false;
    }
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    AsciiDocTable table = AsciiDocTableEditAction.findTable(editor, file);
    return table != null && table.canAppendRow();
  }

  private @Nullable Integer findTarget(@NotNull Editor editor, @NotNull Caret caret) {
    Project project = editor.getProject();
    if (project == null || caret.hasSelection() || editor.getCaretModel().getCaretCount() > 1) {
      return null;
    }
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null || file.getLanguage() != AsciiDocLanguage.INSTANCE) {
      return null;
    }
    return AsciiDocTable.findNeighborCellOffset(file, caret.getOffset(), forward);
  }
}

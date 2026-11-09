package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Base class for actions that edit the table at the caret: insert, delete and move rows and columns.
 */
public abstract class AsciiDocTableEditAction extends AnAction implements DumbAware {
  private final Predicate<AsciiDocTable> allowed;
  private final Consumer<AsciiDocTable> edit;

  protected AsciiDocTableEditAction(Predicate<AsciiDocTable> allowed, Consumer<AsciiDocTable> edit) {
    this.allowed = allowed;
    this.edit = edit;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    AsciiDocTable table = findTable(e.getData(CommonDataKeys.EDITOR), e.getData(CommonDataKeys.PSI_FILE));
    boolean enabled = table != null && allowed.test(table);
    e.getPresentation().setEnabledAndVisible(enabled);
  }

  public static @Nullable AsciiDocTable findTable(@Nullable Editor editor, @Nullable PsiFile file) {
    if (editor == null || file == null || file.getLanguage() != AsciiDocLanguage.INSTANCE) {
      return null;
    }
    return AsciiDocTable.parse(file, editor.getCaretModel().getOffset());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (project == null || editor == null) {
      return;
    }
    perform(project, editor, allowed, edit, getTemplateText());
  }

  public static void perform(@NotNull Project project, @NotNull Editor editor, Predicate<AsciiDocTable> allowed,
                             Consumer<AsciiDocTable> edit, @Nullable String commandName) {
    Document document = editor.getDocument();
    PsiDocumentManager.getInstance(project).commitDocument(document);
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    AsciiDocTable table = findTable(editor, file);
    if (table == null || !allowed.test(table)) {
      return;
    }
    edit.accept(table);
    TextRange body = table.getBodyRange();
    TextRange cols = table.getColsRange();
    String colsValue = table.renderCols();
    String bodyText = table.renderBody();
    WriteCommandAction.runWriteCommandAction(project, commandName, null, () -> {
      document.replaceString(body.getStartOffset(), body.getEndOffset(), bodyText);
      if (cols != null && colsValue != null) {
        document.replaceString(cols.getStartOffset(), cols.getEndOffset(), colsValue);
      }
      int shift = cols != null && colsValue != null ? colsValue.length() - cols.getLength() : 0;
      editor.getCaretModel().moveToOffset(body.getStartOffset() + shift + table.caretOffsetInBody());
    }, file);
  }

  public static class InsertRowAbove extends AsciiDocTableEditAction {
    public InsertRowAbove() {
      super(AsciiDocTable::canInsertRowAbove, t -> t.insertRow(true));
    }
  }

  public static class InsertRowBelow extends AsciiDocTableEditAction {
    public InsertRowBelow() {
      super(AsciiDocTable::canInsertRowBelow, t -> t.insertRow(false));
    }
  }

  public static class DeleteRow extends AsciiDocTableEditAction {
    public DeleteRow() {
      super(AsciiDocTable::canDeleteRow, AsciiDocTable::deleteRow);
    }
  }

  public static class MoveRowUp extends AsciiDocTableEditAction {
    public MoveRowUp() {
      super(AsciiDocTable::canMoveRowUp, t -> t.moveRow(true));
    }
  }

  public static class MoveRowDown extends AsciiDocTableEditAction {
    public MoveRowDown() {
      super(AsciiDocTable::canMoveRowDown, t -> t.moveRow(false));
    }
  }

  public static class InsertColumnLeft extends AsciiDocTableEditAction {
    public InsertColumnLeft() {
      super(AsciiDocTable::canInsertColumn, t -> t.insertColumn(true));
    }
  }

  public static class InsertColumnRight extends AsciiDocTableEditAction {
    public InsertColumnRight() {
      super(AsciiDocTable::canInsertColumn, t -> t.insertColumn(false));
    }
  }

  public static class DeleteColumn extends AsciiDocTableEditAction {
    public DeleteColumn() {
      super(AsciiDocTable::canDeleteColumn, AsciiDocTable::deleteColumn);
    }
  }

  public static class MoveColumnLeft extends AsciiDocTableEditAction {
    public MoveColumnLeft() {
      super(AsciiDocTable::canMoveColumnLeft, t -> t.moveColumn(true));
    }
  }

  public static class MoveColumnRight extends AsciiDocTableEditAction {
    public MoveColumnRight() {
      super(AsciiDocTable::canMoveColumnRight, t -> t.moveColumn(false));
    }
  }
}

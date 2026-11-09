package org.asciidoc.intellij.actions.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.actions.asciidoc.AsciiDocTable;
import org.asciidoc.intellij.actions.asciidoc.AsciiDocTableEditAction;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Intentions with sub-menus to insert, delete and move rows and columns of the table at the caret.
 */
public abstract class AsciiDocTableIntention implements IntentionAction {
  private final String text;
  private final List<Step> steps;

  protected AsciiDocTableIntention(String text, List<Step> steps) {
    this.text = text;
    this.steps = steps;
  }

  @Override
  public @NotNull String getText() {
    return text;
  }

  @Override
  public @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.table.intention.family.name");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return editor != null && AsciiDocFileType.INSTANCE == file.getFileType()
      && !getAvailableOptions(project, editor, file).isEmpty();
  }

  public @NotNull List<IntentionAction> getAvailableOptions(@NotNull Project project, Editor editor, @NotNull PsiFile file) {
    if (editor == null || AsciiDocFileType.INSTANCE != file.getFileType()) {
      return List.of();
    }
    AsciiDocTable table = AsciiDocTableEditAction.findTable(editor, file);
    if (table == null) {
      return List.of();
    }
    return steps.stream().filter(s -> s.allowed.test(table)).map(IntentionAction.class::cast).toList();
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
    showPopup(text, getAvailableOptions(project, editor, file), project, editor, file);
  }

  /**
   * All table row and column actions that are possible at the caret.
   */
  public static @NotNull List<IntentionAction> getAllAvailableOptions(@NotNull Project project, Editor editor, @NotNull PsiFile file) {
    List<IntentionAction> result = new ArrayList<>(new Rows().getAvailableOptions(project, editor, file));
    result.addAll(new Columns().getAvailableOptions(project, editor, file));
    return result;
  }

  public static void showPopup(String title, List<IntentionAction> options, @NotNull Project project, Editor editor, PsiFile file) {
    if (options.isEmpty()) {
      return;
    }
    JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<>(title, options) {
      @Override
      public @NotNull String getTextFor(IntentionAction value) {
        return value.getText();
      }

      @Override
      public PopupStep<?> onChosen(IntentionAction selectedValue, boolean finalChoice) {
        return doFinalStep(() -> selectedValue.invoke(project, editor, file));
      }
    }).showInBestPositionFor(editor);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  static final class Step implements IntentionAction {
    private final String text;
    private final Predicate<AsciiDocTable> allowed;
    private final Consumer<AsciiDocTable> edit;

    Step(String text, Predicate<AsciiDocTable> allowed, Consumer<AsciiDocTable> edit) {
      this.text = text;
      this.allowed = allowed;
      this.edit = edit;
    }

    @Override
    public @NotNull String getText() {
      return text;
    }

    @Override
    public @NotNull String getFamilyName() {
      return AsciiDocBundle.message("asciidoc.table.intention.family.name");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
      AsciiDocTable table = editor == null ? null : AsciiDocTableEditAction.findTable(editor, file);
      return table != null && allowed.test(table);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
      AsciiDocTableEditAction.perform(project, editor, allowed, edit, text);
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }
  }

  public static class Rows extends AsciiDocTableIntention {
    public Rows() {
      super("Table: edit rows...", List.of(
      new Step("Insert row above", AsciiDocTable::canInsertRowAbove, t -> t.insertRow(true)),
      new Step("Insert row below", AsciiDocTable::canInsertRowBelow, t -> t.insertRow(false)),
      new Step("Delete row", AsciiDocTable::canDeleteRow, AsciiDocTable::deleteRow),
      new Step("Move row up", AsciiDocTable::canMoveRowUp, t -> t.moveRow(true)),
      new Step("Move row down", AsciiDocTable::canMoveRowDown, t -> t.moveRow(false))));
    }
  }

  public static class Columns extends AsciiDocTableIntention {
    public Columns() {
      super("Table: edit columns...", List.of(
      new Step("Insert column left", AsciiDocTable::canInsertColumn, t -> t.insertColumn(true)),
      new Step("Insert column right", AsciiDocTable::canInsertColumn, t -> t.insertColumn(false)),
      new Step("Delete column", AsciiDocTable::canDeleteColumn, AsciiDocTable::deleteColumn),
      new Step("Move column left", AsciiDocTable::canMoveColumnLeft, t -> t.moveColumn(true)),
      new Step("Move column right", AsciiDocTable::canMoveColumnRight, t -> t.moveColumn(false))));
    }
  }
}

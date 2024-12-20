package org.asciidoc.intellij.formatting;

import com.intellij.codeInsight.editorActions.AutoHardWrapHandler;
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This handler copies the auto-wrap-in-progress-flag from the handler-specific data context to the editor's data-content,
 * as the handler-specific data context is not available in the {@link AsciiDocLineIndentProvider}.
 *
 * @author Alexander Schwartz
 */
public class AsciiDocEnterHandlerDelegate implements EnterHandlerDelegate {
  @Override
  public Result preprocessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull Ref<Integer> caretOffset, @NotNull Ref<Integer> caretAdvance, @NotNull DataContext dataContext, @Nullable EditorActionHandler originalHandler) {
    if (file.getFileType() == AsciiDocFileType.INSTANCE) {
      if (DataManager.getInstance().loadFromDataContext(dataContext, AutoHardWrapHandler.AUTO_WRAP_LINE_IN_PROGRESS_KEY) == Boolean.TRUE) {
        editor.putUserData(AutoHardWrapHandler.AUTO_WRAP_LINE_IN_PROGRESS_KEY, true);
      }
    }
    return EnterHandlerDelegate.Result.Continue;
  }

  @Override
  public Result postProcessEnter(@NotNull PsiFile file, @NotNull Editor editor, @NotNull DataContext dataContext) {
    if (file.getFileType() == AsciiDocFileType.INSTANCE) {
      if (editor.getUserData(AutoHardWrapHandler.AUTO_WRAP_LINE_IN_PROGRESS_KEY) == Boolean.TRUE) {
        editor.putUserData(AutoHardWrapHandler.AUTO_WRAP_LINE_IN_PROGRESS_KEY, null);
      }
    }
    return EnterHandlerDelegate.Result.Continue;
  }
}

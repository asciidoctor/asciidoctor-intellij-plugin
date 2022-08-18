package org.asciidoc.intellij.actions;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Show the autocomplete lookup list automatically in places where it could be helpful to the user.
 */
public class AsciiDocAntoraTriggerAutoCompleteTypedHandler extends TypedHandlerDelegate {

  @Override
  public @NotNull Result charTyped(char charTyped, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (file.getFileType().getName().equals("YAML") && file.getName().equals("antora.yml")) {
      if (charTyped == '/' // for nav files
      ) {
        // for now, always try to show the lookup. The condition could try to be more specific in the future.
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, null);
      }
    }
    return Result.CONTINUE;
  }
}

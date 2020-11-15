package org.asciidoc.intellij.actions;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

/**
 * Show the autocomplete lookup list automatically in places where it could be helpful to the user.
 */
public class AsciiDocTriggerAutoCompleteTypedHandler extends TypedHandlerDelegate {

  @Override
  public @NotNull Result charTyped(char charTyped, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    if (file.getFileType().equals(AsciiDocFileType.INSTANCE)) {
      // will not see single quotes (') and double quotes (") as they will be handled by the quote handler
      if (charTyped == ':' // might be the start of an attribute declaration: ':attribute:', or the body of a macro: 'xref:file.adoc[]`
        || charTyped == '{' // might be the start of an attribute reference: '{attribute}'
        || charTyped == '<' // might be the start of an anchor reference: '<<anchor>>'
        || charTyped == '#' // might be the start of an anchor in an xref: 'xref:file.adoc#anchor[]'
        || charTyped == '=' // might be the start of a tag like 'include::file.adoc[tag=name]'
      ) {
        // for now, always try to show the lookup. The condition could try to be more specific in the future.
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, null);
      }
    }
    return Result.CONTINUE;
  }
}

package org.asciidoc.intellij.actions;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.SelectionQuotingTypedHandler;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.actions.asciidoc.FormatAsciiDocAction;
import org.jetbrains.annotations.NotNull;

/**
 * When pressing a formatting character like '*', '`', '#' or '_', the selection will be wrapped (or unwrapped)
 * with the formatting given an AsciiDoc document.
 *
 * Heavily inspired by {@link SelectionQuotingTypedHandler}, but different as it toggles the formatting.
 */
public class FormattingQuotedTypedHandler extends TypedHandlerDelegate {

  @NotNull
  @Override
  public Result beforeSelectionRemoved(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    SelectionModel selectionModel = editor.getSelectionModel();
    if (file.getLanguage().equals(AsciiDocLanguage.INSTANCE) &&
      CodeInsightSettings.getInstance().SURROUND_SELECTION_ON_QUOTE_TYPED && selectionModel.hasSelection() && isDelimiter(c)) {
      String selectedText = selectionModel.getSelectedText();
      if (!StringUtil.isEmpty(selectedText)) {
        int selectionStart = selectionModel.getSelectionStart();
        int selectionEnd = selectionModel.getSelectionEnd();
        // expand the selection based on the typed character
        while (selectionStart > 0
          && editor.getDocument().getText(TextRange.create(selectionStart - 1, selectionStart)).equals(String.valueOf(c))
          && selectionEnd < editor.getDocument().getTextLength()
          && editor.getDocument().getText(TextRange.create(selectionEnd, selectionEnd + 1)).equals(String.valueOf(c))) {
          --selectionStart;
          ++selectionEnd;
          selectionModel.setSelection(selectionStart, selectionEnd);
          selectedText = selectionModel.getSelectedText();
        }
        // either remove the formatting...
        char firstChar = selectedText.charAt(0);
        char lastChar = selectedText.charAt(selectedText.length() - 1);
        String newText = null;
        while (selectedText.length() > 2 && firstChar == lastChar && firstChar == c
          && isDelimiter(firstChar) && !SelectionQuotingTypedHandler.shouldSkipReplacementOfQuotesOrBraces(file, editor, selectedText, c)) {
          selectedText = selectedText.substring(1, selectedText.length() - 1);
          firstChar = selectedText.charAt(0);
          lastChar = selectedText.charAt(selectedText.length() - 1);
          newText = selectedText;
        }
        // ... or add it around the text
        int border = 0;
        if (newText == null) {
          boolean word = FormatAsciiDocAction.isWord(editor.getDocument(), selectionStart, selectionEnd, "" + c);
          if (word || c == '"' || c == '\'' || c == '~' || c == '^') {
            newText = c + selectedText + c;
            border = 1;
          } else {
            // prefix a string to prevent that adding two chars results in an integer
            newText = "" + c + c + selectedText + c + c;
            border = 2;
          }
        }
        final int caretOffset = selectionModel.getSelectionStart();
        boolean ltrSelection = selectionModel.getLeadSelectionOffset() != selectionModel.getSelectionEnd();
        boolean restoreStickySelection = editor instanceof EditorEx && ((EditorEx) editor).isStickySelection();
        selectionModel.removeSelection();
        editor.getDocument().replaceString(selectionStart, selectionEnd, newText);
        TextRange replacedTextRange = new TextRange(caretOffset + border, caretOffset + newText.length() - border);
        // selection is removed here
        if (replacedTextRange.getEndOffset() <= editor.getDocument().getTextLength()) {
          if (restoreStickySelection) {
            EditorEx editorEx = (EditorEx) editor;
            CaretModel caretModel = editorEx.getCaretModel();
            caretModel.moveToOffset(ltrSelection ? replacedTextRange.getStartOffset() : replacedTextRange.getEndOffset());
            editorEx.setStickySelection(true);
            caretModel.moveToOffset(ltrSelection ? replacedTextRange.getEndOffset() : replacedTextRange.getStartOffset());
          } else {
            if (ltrSelection || editor instanceof EditorWindow) {
              editor.getSelectionModel().setSelection(replacedTextRange.getStartOffset(), replacedTextRange.getEndOffset());
            } else {
              editor.getSelectionModel().setSelection(replacedTextRange.getEndOffset(), replacedTextRange.getStartOffset());
            }
            editor.getCaretModel().moveToOffset(ltrSelection ? replacedTextRange.getEndOffset() : replacedTextRange.getStartOffset());
          }
        }
        return Result.STOP;
      }
    }
    return super.beforeSelectionRemoved(c, project, editor, file);
  }

  private static boolean isDelimiter(final char c) {
    return c == '*' || c == '_' || c == '#' || c == '`' || c == '"' || c == '\'' || c == '$' || c == '+' || c == '^' || c == '~';
  }

}

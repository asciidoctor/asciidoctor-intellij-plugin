package org.asciidoc.intellij.actions.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.macros.AsciiDocMacroAttributes;
import org.asciidoc.intellij.psi.AsciiDocAttributeInBrackets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts a positional attribute of a macro like <code>image::</code> to a named attribute.
 */
public class AsciiDocMacroPositionalToNamedIntention extends Intention {

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return getName(editor, file) != null;
  }

  private static @Nullable String getName(Editor editor, PsiFile file) {
    if (editor == null || file == null || AsciiDocFileType.INSTANCE != file.getFileType()) {
      return null;
    }
    AsciiDocAttributeInBrackets attribute = getAttributeAtCaret(editor, file);
    if (attribute == null || AsciiDocMacroAttributes.isNamed(attribute)) {
      return null;
    }
    PsiElement macroElement = attribute.getParent();
    AsciiDocMacroAttributes.Macro macro = AsciiDocMacroAttributes.find(macroElement);
    if (macro == null) {
      return null;
    }
    int position = AsciiDocMacroAttributes.getPosition(attribute);
    if (position < 0 || position >= macro.positional().size()) {
      return null;
    }
    String name = macro.positional().get(position);
    if (AsciiDocMacroAttributes.getUsedNames(macroElement, attribute).contains(name)) {
      return null;
    }
    return name;
  }

  static @Nullable AsciiDocAttributeInBrackets getAttributeAtCaret(Editor editor, PsiFile file) {
    int offset = editor.getCaretModel().getOffset();
    for (int o : new int[]{offset, offset - 1}) {
      if (o < 0) {
        continue;
      }
      PsiElement element = file.findElementAt(o);
      AsciiDocAttributeInBrackets attribute = PsiTreeUtil.getParentOfType(element, AsciiDocAttributeInBrackets.class, false);
      if (attribute != null) {
        return attribute;
      }
    }
    return null;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    String name = getName(editor, file);
    AsciiDocAttributeInBrackets attribute = getAttributeAtCaret(editor, file);
    if (name == null || attribute == null) {
      return;
    }
    editor.getDocument().insertString(attribute.getTextRange().getStartOffset(), name + "=");
    PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}

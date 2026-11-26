package org.asciidoc.intellij.actions.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.macros.AsciiDocMacroAttributes;
import org.asciidoc.intellij.psi.AsciiDocAttributeInBrackets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a named attribute of a macro like <code>image::</code> to a positional attribute.
 * The attribute list is re-ordered: the attribute is moved to its position, all other positional attributes keep
 * their position, and the remaining named attributes follow.
 */
public class AsciiDocMacroNamedToPositionalIntention extends Intention {

  private record Change(TextRange range, String text) {
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return getChange(editor, file) != null;
  }

  private static @Nullable Change getChange(Editor editor, PsiFile file) {
    if (editor == null || file == null || AsciiDocFileType.INSTANCE != file.getFileType()) {
      return null;
    }
    AsciiDocAttributeInBrackets target = AsciiDocMacroPositionalToNamedIntention.getAttributeAtCaret(editor, file);
    if (target == null || !AsciiDocMacroAttributes.isNamed(target)) {
      return null;
    }
    PsiElement macroElement = target.getParent();
    AsciiDocMacroAttributes.Macro macro = AsciiDocMacroAttributes.find(macroElement);
    String name = AsciiDocMacroAttributes.getName(target);
    int targetSlot = name == null ? -1 : macro == null ? -1 : macro.positional().indexOf(name);
    if (targetSlot < 0) {
      return null;
    }
    String text = target.getText();
    String value = text.substring(text.indexOf('=') + 1).trim();
    if (value.isEmpty()) {
      return null;
    }

    int start = -1;
    int end = -1;
    List<String> slots = new ArrayList<>();
    slots.add(null);
    List<String> named = new ArrayList<>();
    for (PsiElement child = macroElement.getFirstChild(); child != null; child = child.getNextSibling()) {
      var type = child.getNode().getElementType();
      if (type == AsciiDocTokenTypes.ATTRS_START || type == AsciiDocTokenTypes.INLINE_ATTRS_START) {
        start = child.getTextRange().getEndOffset();
      } else if (type == AsciiDocTokenTypes.ATTRS_END || type == AsciiDocTokenTypes.INLINE_ATTRS_END) {
        end = child.getTextRange().getStartOffset();
      } else if (start >= 0 && end < 0) {
        if (type == AsciiDocTokenTypes.SEPARATOR && child.getText().equals(",")) {
          slots.add(null);
        } else if (child instanceof AsciiDocAttributeInBrackets attribute) {
          String attributeText = attribute.getText().trim();
          if (attribute == target) {
            continue;
          }
          if (AsciiDocMacroAttributes.isNamed(attribute)) {
            named.add(attributeText);
          } else {
            slots.set(slots.size() - 1, attributeText);
          }
        } else if (type != AsciiDocTokenTypes.WHITE_SPACE) {
          return null;
        }
      }
    }
    if (start < 0 || end < start) {
      return null;
    }
    if (targetSlot < slots.size() && slots.get(targetSlot) != null) {
      // the position is already used by another attribute
      return null;
    }
    while (slots.size() <= targetSlot) {
      slots.add(null);
    }
    slots.set(targetSlot, value);
    while (slots.getLast() == null) {
      slots.removeLast();
    }

    List<String> parts = new ArrayList<>();
    for (String slot : slots) {
      parts.add(slot == null ? "" : slot);
    }
    parts.addAll(named);
    return new Change(new TextRange(start, end), String.join(",", parts));
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    Change change = getChange(editor, file);
    if (change == null) {
      return;
    }
    editor.getDocument().replaceString(change.range().getStartOffset(), change.range().getEndOffset(), change.text());
    PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}

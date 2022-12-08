package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocAddDescriptionPageAttribute extends AsciiDocLocalQuickFix {

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.addDecriptionPageAttribute");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement anchor = descriptor.getPsiElement();
    String text = ":description: TODO: add description here\n";
    // go to the start of the next line
    boolean nlFound = false;
    while (anchor.getNextSibling() instanceof PsiWhiteSpace) {
      anchor = anchor.getNextSibling();
      if (anchor.getText().equals("\n")) {
        nlFound = true;
        break;
      }
    }
    if (!nlFound) {
      PsiElement newline = createAttributeDeclaration(project, "\n");
      anchor = anchor.getParent().addRangeAfter(newline.getFirstChild(), newline.getLastChild(), anchor).getParent();
      anchor = anchor.getNextSibling();
    }
    if (anchor.getNextSibling() instanceof PsiWhiteSpace && anchor.getNextSibling().getText().equals("\n")) {
      // there was a blank new line following the heading, keep it a blank line
      text = text + "\n";
    }
    PsiElement attribute = createAttributeDeclaration(project, text);
    anchor = anchor.getParent().addRangeAfter(attribute.getFirstChild(), attribute.getLastChild(), anchor);
    while (anchor instanceof PsiWhiteSpace) {
      anchor = anchor.getNextSibling();
    }
    anchor = anchor.getLastChild();
    ((Navigatable) anchor).navigate(false);
    FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor(anchor.getContainingFile().getVirtualFile());
    if (selectedEditor instanceof TextEditor) {
      Editor editor = ((TextEditor) selectedEditor).getEditor();
      editor.getSelectionModel().setSelection(anchor.getTextOffset(), anchor.getTextOffset() + anchor.getTextLength());
    }
  }

  @NotNull
  private static PsiElement createAttributeDeclaration(@NotNull Project project, @NotNull String text) {
    return AsciiDocUtil.createFileFromText(project, text);
  }
}

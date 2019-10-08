package org.asciidoc.intellij.actions.asciidoc;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocUrl;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alexander Schwartz
 */
public class MakeLink extends FormatAsciiDocAction {
  @Override
  public void update(@NotNull AnActionEvent event) {
    super.update(event);
    if (event.getPresentation().isEnabled()) {
      PsiFile file = event.getData(LangDataKeys.PSI_FILE);
      Editor editor = event.getData(LangDataKeys.EDITOR);
      if (file != null && editor != null) {
        PsiElement statementAtCaret = AsciiDocUtil.getStatementAtCaret(editor, file);
        if (statementAtCaret != null) {
          statementAtCaret = statementAtCaret.getParent();
        }
        if (statementAtCaret instanceof AsciiDocUrl && ((AsciiDocUrl) statementAtCaret).hasText()) {
          event.getPresentation().setEnabled(false);
        }
        if (statementAtCaret instanceof AsciiDocLink) {
          event.getPresentation().setEnabled(false);
        }
      }
    }
  }

  @Override
  public String getName() {
    return "MakeLink";
  }

  @Override
  public String updateSelection(String selection, boolean word) {
    return null; // dummy implementation to satisfy base class
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {

    final Project project = event.getProject();
    if (project == null) {
      return;
    }
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor == null) {
      return;
    }
    final Document document = editor.getDocument();

    WriteCommandAction.runWriteCommandAction(project, () -> {
      selectText(editor);
      updateDocument(document, editor);
    });
  }

  @Override
  protected void selectText(Editor editor) {
    SelectionModel selectionModel = editor.getSelectionModel();
    if (!selectionModel.hasSelection() && editor.getProject() != null) {
      PsiFile psiFile = PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument());
      if (psiFile != null) {
        PsiElement statementAtCaret = AsciiDocUtil.getStatementAtCaret(editor, psiFile);
        if (statementAtCaret != null && (statementAtCaret.getNode().getElementType() == AsciiDocTokenTypes.URL_LINK
          || statementAtCaret.getNode().getElementType() == AsciiDocTokenTypes.URL_EMAIL)) {
          int start = statementAtCaret.getTextOffset();
          int end = statementAtCaret.getTextOffset() + statementAtCaret.getTextLength();
          selectionModel.setSelection(start, end);
          return;
        }
      }
    }
    super.selectText(editor);
  }

  private void updateDocument(Document document, Editor editor) {
    CaretModel caretModel = editor.getCaretModel();
    SelectionModel selectionModel = editor.getSelectionModel();
    if (isLink(editor)) {
      removeUrlStartEnd(editor);
      String dummyText = "text";
      document.insertString(selectionModel.getSelectionEnd(), "[" + dummyText + "]");
      caretModel.moveToOffset(selectionModel.getSelectionEnd() + 1);
      selectionModel.setSelection(selectionModel.getSelectionEnd() + 1, selectionModel.getSelectionEnd() + dummyText.length() + 1);
    } else if (isEmail(editor)) {
      String dummyText = "text";
      document.insertString(selectionModel.getSelectionStart(), "mailto:");
      document.insertString(selectionModel.getSelectionEnd(), "[" + dummyText + "]");
      caretModel.moveToOffset(selectionModel.getSelectionEnd() + 1);
      selectionModel.setSelection(selectionModel.getSelectionEnd() + 1, selectionModel.getSelectionEnd() + dummyText.length() + 1);
    } else {
      String dummyLink = "https://xxx";
      document.insertString(selectionModel.getSelectionEnd(), "]");
      document.insertString(selectionModel.getSelectionStart(), dummyLink + "[");
      if (selectionModel.getSelectionStart() != selectionModel.getSelectionEnd()) {
        selectionModel.setSelection(selectionModel.getSelectionStart() - 1 - dummyLink.length(), selectionModel.getSelectionStart() - 1);
      } else {
        selectionModel.setSelection(selectionModel.getSelectionStart(), selectionModel.getSelectionStart() + dummyLink.length());
      }
      caretModel.moveToOffset(selectionModel.getSelectionStart());
    }
  }

  private boolean isLink(Editor editor) {
    if (editor.getProject() != null) {
      PsiFile psiFile = PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument());
      if (psiFile != null) {
        PsiElement statementAtCaret = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());
        return statementAtCaret != null && statementAtCaret.getNode().getElementType() == AsciiDocTokenTypes.URL_LINK;
      }
    }
    return false;
  }

  private boolean isEmail(Editor editor) {
    if (editor.getProject() != null) {
      PsiFile psiFile = PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument());
      if (psiFile != null) {
        PsiElement statementAtCaret = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());
        return statementAtCaret != null && statementAtCaret.getNode().getElementType() == AsciiDocTokenTypes.URL_EMAIL;
      }
    }
    return false;
  }

  private void removeUrlStartEnd(Editor editor) {
    if (editor.getProject() != null) {
      PsiFile psiFile = PsiDocumentManager.getInstance(editor.getProject()).getPsiFile(editor.getDocument());
      PsiDocumentManager.getInstance(editor.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument());
      if (psiFile != null) {
        PsiElement statementAtCaret = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());
        if (statementAtCaret != null && statementAtCaret.getNode().getElementType() == AsciiDocTokenTypes.URL_LINK) {
          if (statementAtCaret.getPrevSibling() != null && statementAtCaret.getPrevSibling().getNode().getElementType() == AsciiDocTokenTypes.URL_START
            && statementAtCaret.getNextSibling() != null && statementAtCaret.getNextSibling().getNode().getElementType() == AsciiDocTokenTypes.URL_END) {
            statementAtCaret.getPrevSibling().delete();
            statementAtCaret.getNextSibling().delete();
            PsiDocumentManager.getInstance(editor.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument());
          }
        }
      }
    }
  }
}

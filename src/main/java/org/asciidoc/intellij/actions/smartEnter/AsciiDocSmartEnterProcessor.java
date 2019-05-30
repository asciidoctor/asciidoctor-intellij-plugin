package org.asciidoc.intellij.actions.smartEnter;

import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocSmartEnterProcessor extends SmartEnterProcessor {
  private static final Logger LOG = Logger.getInstance(AsciiDocSmartEnterProcessor.class);

  @Override
  public boolean process(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile psiFile) {
    return completeEndTag(project, editor, psiFile);
  }

  private boolean completeEndTag(Project project, Editor editor, PsiFile psiFile) {
    PsiElement atCaret = getStatementAtCaret(editor, psiFile);

    if (atCaret == null) {
      return false;
    }

    final Document doc = editor.getDocument();
    int caretTo;
    boolean result = false;

    try {
      if (atCaret.getNode() != null) {

        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER
          || atCaret.getNode().getElementType() == AsciiDocTokenTypes.QUOTE_BLOCK_DELIMITER
          || atCaret.getNode().getElementType() == AsciiDocTokenTypes.EXAMPLE_BLOCK_DELIMITER
          || atCaret.getNode().getElementType() == AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER
          || atCaret.getNode().getElementType() == AsciiDocTokenTypes.SIDEBAR_BLOCK_DELIMITER
          || atCaret.getNode().getElementType() == AsciiDocTokenTypes.COMMENT_BLOCK_DELIMITER) {
          String textToInsert = "\n\n" + atCaret.getText() + "\n";
          doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
          caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + 1;
          commitChanges(project, editor, caretTo);
          result = true;
        }
        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.BLOCK_ATTR_NAME) {
          if (atCaret.getNextSibling() == null) {
            String textToInsert = "]";
            doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
            caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + 1;
            commitChanges(project, editor, caretTo);
            result = true;
          }
          if (atCaret.getNextSibling().getNode().getElementType() == AsciiDocTokenTypes.BLOCK_ATTRS_END) {
            atCaret = atCaret.getNextSibling();
          }
        }
        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.BLOCK_ATTRS_END) {
          ASTNode[] attr = atCaret.getParent().getNode().getChildren(TokenSet.create(AsciiDocTokenTypes.BLOCK_ATTR_NAME));
          if (attr.length > 0 && "source".equals(attr[0].getText())) {
            String textToInsert = "\n----\n\n----";
            doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
            caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + 6;
            commitChanges(project, editor, caretTo);
            result = true;
          }
          if (attr.length > 0 && "plantuml".equals(attr[0].getText())) {
            String textToInsert = "\n----\n@startuml\n\n@enduml\n----";
            doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
            caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + 16;
            commitChanges(project, editor, caretTo);
            result = true;
          }
        }
      }
    } catch (IncorrectOperationException e) {
      LOG.error(e);
    }
    return result;

  }

  private void commitChanges(Project project, Editor editor, int caretOffset) {
    if (isUncommited(project)) {
      commit(editor);
      editor.getCaretModel().moveToOffset(caretOffset);
    }
    commit(editor);
  }
}

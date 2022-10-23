package org.asciidoc.intellij.actions.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Rewriting a single-line admonition to a block style admonition.
 *
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocAdmonitionToBlockIntention extends Intention {

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    if (AsciiDocFileType.INSTANCE != file.getFileType()) {
      return false;
    }
    PsiElement statementAtCaret = AsciiDocUtil.getStatementAtCaret(editor, file);
    if (statementAtCaret == null) {
      return false;
    }
    if (statementAtCaret.getNode() != null && statementAtCaret.getNode().getElementType() != AsciiDocTokenTypes.ADMONITION) {
      return false;
    }
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PsiElement statementAtCaret = AsciiDocUtil.getStatementAtCaret(editor, file);
    if (statementAtCaret == null) {
      return;
    }
    if (statementAtCaret.getNode() != null && statementAtCaret.getNode().getElementType() != AsciiDocTokenTypes.ADMONITION) {
      return;
    }
    PsiElement loop = statementAtCaret.getNextSibling();
    StringBuilder buffer = new StringBuilder();
    if (loop != null) {
      while (loop instanceof PsiWhiteSpace) {
        loop = loop.getNextSibling();
      }
      PsiElement end = loop;
      boolean onNewLine = false;
      while (loop != null) {
        if (loop instanceof PsiWhiteSpace) {
          // unfortunately the original token type from lexing is lost; it is always "Whitespace"
          if (onNewLine) {
            // found a new line on an empty line, stop here
            if (loop.getText().equals("\n")) {
              break;
            }
          }
          if (loop.getText().equals("\n")) {
            onNewLine = true;
          }
        } else {
          onNewLine = false;
        }
        if (loop instanceof AsciiDocBlock || (loop.getNode() != null && loop.getNode().getElementType() == AsciiDocTokenTypes.CONTINUATION)) {
          break;
        }
        buffer.append(loop.getText());
        end = loop;
        loop = loop.getNextSibling();
      }
      statementAtCaret.getParent().deleteChildRange(statementAtCaret.getNextSibling(), end);
    }

    if (!buffer.toString().endsWith("\n")) {
      buffer.append("\n");
    }
    buffer.append("====\n");
    buffer.insert(0, "]\n====\n");
    buffer.insert(0, statementAtCaret.getText().substring(0, statementAtCaret.getText().length() - 1));
    buffer.insert(0, "[");

    PsiElement newBlock = createAdmonitionBlock(project, buffer.toString()).getFirstChild();

    PsiElement newElement = statementAtCaret.replace(newBlock);
    newBlock = newBlock.getNextSibling();
    newElement.getParent().addRangeAfter(newBlock, newBlock.getParent().getLastChild(), newElement);
  }

  @NotNull
  private static PsiElement createAdmonitionBlock(@NotNull Project project, @NotNull String text) {
    return AsciiDocUtil.createFileFromText(project, text);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}

package org.asciidoc.intellij.actions.smartEnter;

import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.psi.AsciiDocAttributeInBrackets;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

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
        if (atCaret.getTextRange().getEndOffset() == editor.getDocument().getTextLength()
          && !(atCaret instanceof PsiWhiteSpace)) {
          // as new line is necessary at end of file for delimiters to be recognized
          String textToInsert = "\n";
          doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
          commitChanges(project, editor, editor.getCaretModel().getOffset());
          // get updated PsiElement as it might have changed due to the newline
          atCaret = getStatementAtCaret(editor, psiFile);
          if (atCaret == null || atCaret.getNode() == null) {
            return result;
          }
          result = true;
        }
        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER
          || atCaret.getNode().getElementType() == AsciiDocTokenTypes.BLOCK_DELIMITER
          || atCaret.getNode().getElementType() == AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER
          || atCaret.getNode().getElementType() == AsciiDocTokenTypes.COMMENT_BLOCK_DELIMITER) {
          String textToInsert = "\n\n" + atCaret.getText();
          doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
          caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + 1;
          commitChanges(project, editor, caretTo);
          result = true;
        }
        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.ATTR_NAME) {
          if (atCaret.getNextSibling() == null) {
            String textToInsert = "]";
            doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
            caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + 1;
            commitChanges(project, editor, caretTo);
            result = true;
          }
          if (atCaret.getNextSibling() == null && atCaret.getParent().getNextSibling() == AsciiDocTokenTypes.ATTRS_END) {
            atCaret = atCaret.getParent().getNextSibling();
          }
        }
        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.BLOCK_MACRO_BODY ||
          (atCaret.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_REF_END
            && atCaret.getNode().getTreeParent().getTreeParent().getElementType() == AsciiDocElementTypes.BLOCK_MACRO)) {
          if (atCaret.getNextSibling() == null) {
            String textToInsert = "[";
            doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
            caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + textToInsert.length();
            commitChanges(project, editor, caretTo);
            if (atCaret.getNextSibling() != null) {
              atCaret = atCaret.getNextSibling();
            }
            result = true;
          }
        }
        if ((atCaret.getNode().getElementType() == AsciiDocTokenTypes.ATTRS_START ||
          atCaret.getNode().getElementType() == AsciiDocTokenTypes.SEPARATOR)
          && atCaret.getNode().getTreeParent().getElementType() == AsciiDocElementTypes.BLOCK_MACRO) {
          if (atCaret.getNextSibling() == null) {
            // the default
            String textToInsert = "]\n";
            PsiElement parent = atCaret.getParent();

            // if this is an include-macro, try to add leveloffset-attribute to match
            if (parent instanceof AsciiDocBlockMacro && ((AsciiDocBlockMacro) parent).getMacroName().equals("include")) {
              PsiReference[] references = parent.getReferences();
              if (references.length > 0 && references[references.length - 1] instanceof AsciiDocFileReference) {
                // resolve file reference
                AsciiDocFileReference fileReference = (AsciiDocFileReference) references[references.length - 1];
                PsiElement resolve = fileReference.resolve();
                if (resolve instanceof PsiFile && ((PsiFile) resolve).getFileType() == AsciiDocFileType.INSTANCE) {
                  // get first section of referenced file
                  Collection<AsciiDocSection> childrenOfType = PsiTreeUtil.findChildrenOfType(resolve, AsciiDocSection.class);
                  if (childrenOfType.size() > 0) {
                    AsciiDocSection next = childrenOfType.iterator().next();
                    int includeLevel = next.headingLevel();
                    AsciiDocSection parentSection = (AsciiDocSection) PsiTreeUtil.findFirstParent(parent, psiElement -> psiElement instanceof AsciiDocSection);
                    if (parentSection != null) {
                      // derive section level from current section
                      int parentLevel = parentSection.headingLevel();
                      int delta = parentLevel - includeLevel + 1;
                      if (delta > 0) {
                        textToInsert = "leveloffset=+" + delta + textToInsert;
                      } else if (delta < 0) {
                        textToInsert = "leveloffset=" + delta + textToInsert;
                      }
                    }
                  }
                }
              }
            }
            doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
            caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + textToInsert.length();
            commitChanges(project, editor, caretTo);
            atCaret = getStatementAtCaret(editor, psiFile);
            result = true;
            if (atCaret == null) {
              return result;
            }
          }
        }
        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.ATTRS_END) {
          AsciiDocAttributeInBrackets attr = PsiTreeUtil.findChildOfType(atCaret.getParent(), AsciiDocAttributeInBrackets.class);
          if (attr != null && "source".equals(attr.getAttrName())) {
            String textToInsert = "\n----\n\n----";
            if (atCaret.getTextRange().getEndOffset() == editor.getDocument().getTextLength()) {
              textToInsert = textToInsert + "\n";
            }
            doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
            caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + 6;
            commitChanges(project, editor, caretTo);
            result = true;
          }
          if (attr != null && "plantuml".equals(attr.getAttrName())) {
            String textToInsert = "\n----\n@startuml\n\n@enduml\n----";
            if (atCaret.getTextRange().getEndOffset() == editor.getDocument().getTextLength()) {
              textToInsert = textToInsert + "\n";
            }
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

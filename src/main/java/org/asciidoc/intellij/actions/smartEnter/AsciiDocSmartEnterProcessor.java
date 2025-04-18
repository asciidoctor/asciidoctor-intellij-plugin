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
import org.asciidoc.intellij.psi.AsciiDocBlockId;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class AsciiDocSmartEnterProcessor extends SmartEnterProcessor {
  private static final Logger LOG = Logger.getInstance(AsciiDocSmartEnterProcessor.class);

  @Override
  public boolean process(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile psiFile) {
    return completeEndTag(project, editor, psiFile);
  }

  @SuppressWarnings("checkstyle:MethodLength")
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
          atCaret = getStatementAtCaret(editor, psiFile);
          if (atCaret == null || atCaret.getNode() == null) {
            return result;
          }
          result = true;
        }
        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.ATTR_NAME) {
          if (atCaret.getNextSibling() == null &&
            atCaret.getParent().getNextSibling() != null &&
            atCaret.getParent().getNextSibling().getNode().getElementType() == AsciiDocTokenTypes.ATTRS_END) {
            atCaret = atCaret.getParent().getNextSibling();
          } else if (atCaret.getNextSibling() == null) {
            String textToInsert = "]";
            doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
            caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + 1;
            commitChanges(project, editor, caretTo);
            atCaret = getStatementAtCaret(editor, psiFile);
            if (atCaret == null || atCaret.getNode() == null) {
              return result;
            }
            result = true;
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
            textToInsert = prepareLevelOffset(atCaret, textToInsert);
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

        // incomplete inline macro, add brackets to complete it
        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.TEXT && atCaret.getText().matches("^[a-zA-Z]+:[^]]+")) {
          doc.insertString(atCaret.getTextRange().getEndOffset(), "[]");
          caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + 1;
          commitChanges(project, editor, caretTo);
          atCaret = getStatementAtCaret(editor, psiFile);
          result = true;
          if (atCaret == null) {
            return result;
          }
        }

        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.LBRACKET) {
          if (atCaret.getNode().getTreePrev() != null
            && atCaret.getNode().getTreePrev().getElementType() == AsciiDocTokenTypes.TEXT
            && atCaret.getNode().getTreePrev().getText().matches("^[a-zA-Z]+:[^]]+")) {
            doc.insertString(atCaret.getTextRange().getEndOffset(), "]");
            caretTo = atCaret.getTextOffset() + atCaret.getTextLength();
            commitChanges(project, editor, caretTo);
            atCaret = getStatementAtCaret(editor, psiFile);
            result = true;
            if (atCaret == null) {
              return result;
            }
          }
        }

        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.INLINE_ATTRS_START) {
          if (atCaret.getNode().getTreeNext() != null
            && atCaret.getNode().getTreeNext().getText().matches("^[\n \t].*")) {
            doc.insertString(atCaret.getTextRange().getEndOffset(), "]");
            caretTo = atCaret.getTextOffset() + atCaret.getTextLength();
            commitChanges(project, editor, caretTo);
            atCaret = getStatementAtCaret(editor, psiFile);
            result = true;
            if (atCaret == null) {
              return result;
            }
          }
        }

        if (atCaret.getNode().getElementType() == AsciiDocTokenTypes.INLINE_ATTRS_START) {
          if (atCaret.getNextSibling() != null && atCaret.getNextSibling().getNode().getElementType() == AsciiDocTokenTypes.INLINE_ATTRS_END) {
            String textToInsert = null;
            textToInsert = prepareTitleForLink(atCaret, textToInsert);

            if (textToInsert != null) {
              textToInsert = textToInsert.replaceAll("]", "\\\\]");
              doc.insertString(atCaret.getTextRange().getEndOffset(), textToInsert);
              caretTo = atCaret.getTextOffset() + atCaret.getTextLength() + textToInsert.length() + 1;
              commitChanges(project, editor, caretTo);
              atCaret = getStatementAtCaret(editor, psiFile);
              result = true;
              if (atCaret == null) {
                return result;
              }
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

  private String prepareTitleForLink(PsiElement atCaret, String textToInsert) {
    PsiElement parent = atCaret.getParent();
    // if this is a link-macro, try to add title
    if (parent instanceof AsciiDocLink) {
      PsiReference[] references = parent.getReferences();
      if (references.length > 0 && references[references.length - 1] instanceof AsciiDocFileReference fileReference) {
        // resolve file reference
        if (AsciiDocUtil.findAntoraModuleDir(atCaret) != null && !fileReference.isAnchor()) {
          // for page links, Antora will retrieve the information automatically, therefore, leave it blank.
          return "";
        }
        PsiElement resolve = fileReference.resolve();
        if (resolve instanceof AsciiDocBlockId && resolve.getParent() instanceof AsciiDocSection) {
          resolve = resolve.getParent();
        }
        if (resolve instanceof PsiFile && ((PsiFile) resolve).getFileType() == AsciiDocFileType.INSTANCE) {
          // get first section of referenced file
          Collection<AsciiDocSection> childrenOfType = PsiTreeUtil.findChildrenOfType(resolve, AsciiDocSection.class);
          if (!childrenOfType.isEmpty()) {
            AsciiDocSection next = childrenOfType.iterator().next();
            textToInsert = next.getTitle();
          }
        } else if (resolve instanceof AsciiDocSection) {
          textToInsert = ((AsciiDocSection) resolve).getTitle();
        }
      }
    }
    return textToInsert;
  }

  private String prepareLevelOffset(PsiElement atCaret, String textToInsert) {
    PsiElement parent = atCaret.getParent();

    // if this is an include-macro, try to add leveloffset-attribute to match
    if (parent instanceof AsciiDocBlockMacro && ((AsciiDocBlockMacro) parent).getMacroName().equals("include")) {
      PsiReference[] references = parent.getReferences();
      if (references.length > 0 && references[references.length - 1] instanceof AsciiDocFileReference fileReference) {
        // resolve file reference
        PsiElement resolve = fileReference.resolve();
        if (resolve instanceof PsiFile && ((PsiFile) resolve).getFileType() == AsciiDocFileType.INSTANCE) {
          // get first section of referenced file
          Collection<AsciiDocSection> childrenOfType = PsiTreeUtil.findChildrenOfType(resolve, AsciiDocSection.class);
          if (!childrenOfType.isEmpty()) {
            AsciiDocSection next = childrenOfType.iterator().next();
            int includeLevel = next.getHeadingLevel();
            AsciiDocSection parentSection = (AsciiDocSection) PsiTreeUtil.findFirstParent(parent, psiElement -> psiElement instanceof AsciiDocSection);
            if (parentSection != null) {
              // derive section level from current section
              int parentLevel = parentSection.getHeadingLevel();
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
    return textToInsert;
  }

  private void commitChanges(Project project, Editor editor, int caretOffset) {
    if (isUncommited(project)) {
      commit(editor);
      editor.getCaretModel().moveToOffset(caretOffset);
    }
    commit(editor);
  }
}

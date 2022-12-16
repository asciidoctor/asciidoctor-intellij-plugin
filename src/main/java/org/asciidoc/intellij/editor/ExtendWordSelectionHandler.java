package org.asciidoc.intellij.editor;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRS_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ATTRS_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BOLD_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BOLD_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.DOUBLEBOLD_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.DOUBLEBOLD_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.DOUBLEITALIC_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.DOUBLEITALIC_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.DOUBLEMONO_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.DOUBLEMONO_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.DOUBLE_QUOTE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_ATTRS_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINE_ATTRS_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ITALIC_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ITALIC_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LBRACKET;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LPAREN;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.MONO_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.MONO_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.RBRACKET;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.RPAREN;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.SINGLE_QUOTE;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.SUBSCRIPT_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.SUBSCRIPT_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.SUPERSCRIPT_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.SUPERSCRIPT_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_END;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START;

/**
 * Suggest word selections to the editor.
 * See <code>com.intellij.codeInsight.editorActions.wordSelection.NaturalLanguageTextSelectioner</code>
 * for more ideas.
 */
public class ExtendWordSelectionHandler extends ExtendWordSelectionHandlerBase {
  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    return e.getContainingFile().getLanguage().equals(AsciiDocLanguage.INSTANCE);
  }

  private static final Map<IElementType, IElementType> SYMMETRIC_FORMATTING = new HashMap<>();

  static {
    SYMMETRIC_FORMATTING.put(BOLD_START, BOLD_END);
    SYMMETRIC_FORMATTING.put(SUBSCRIPT_START, SUBSCRIPT_END);
    SYMMETRIC_FORMATTING.put(SUPERSCRIPT_START, SUPERSCRIPT_END);
    SYMMETRIC_FORMATTING.put(DOUBLEBOLD_START, DOUBLEBOLD_END);
    SYMMETRIC_FORMATTING.put(ITALIC_START, ITALIC_END);
    SYMMETRIC_FORMATTING.put(DOUBLEITALIC_START, DOUBLEITALIC_END);
    SYMMETRIC_FORMATTING.put(MONO_START, MONO_END);
    SYMMETRIC_FORMATTING.put(DOUBLEMONO_START, DOUBLEMONO_END);
    SYMMETRIC_FORMATTING.put(SINGLE_QUOTE, SINGLE_QUOTE);
    SYMMETRIC_FORMATTING.put(DOUBLE_QUOTE, DOUBLE_QUOTE);
    SYMMETRIC_FORMATTING.put(LBRACKET, RBRACKET);
    SYMMETRIC_FORMATTING.put(LPAREN, RPAREN);
    SYMMETRIC_FORMATTING.put(ATTRS_START, ATTRS_END);
    SYMMETRIC_FORMATTING.put(INLINE_ATTRS_START, INLINE_ATTRS_END);
    SYMMETRIC_FORMATTING.put(TYPOGRAPHIC_DOUBLE_QUOTE_START, TYPOGRAPHIC_DOUBLE_QUOTE_END);
    SYMMETRIC_FORMATTING.put(TYPOGRAPHIC_SINGLE_QUOTE_START, TYPOGRAPHIC_SINGLE_QUOTE_END);
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
    final TextRange originalRange = e.getTextRange();
    if (originalRange.getEndOffset() > editorText.length()) {
      throw new RuntimeExceptionWithAttachments("Invalid element range in " + getClass(),
        "element=" + e +
          "; range=" + originalRange +
          "; text length=" + editorText.length() +
          "; editor=" + editor +
          "; committed=" + PsiDocumentManager.getInstance(e.getProject()).isCommitted(editor.getDocument()),
        new Attachment("editor_text.txt", editorText.toString()),
        new Attachment("psi_text.txt", e.getText()));
    }
    ArrayList<TextRange> ranges = new ArrayList<>();
    PsiElement element = e;
    do {
      TextRange range = element.getTextRange();
      // add element as is
      ranges.add(range);

      PsiElement startFormatting = element;
      PsiElement endFormatting = element;

      // expand start/endFormatting within paragraph
      while (startFormatting != null && endFormatting != null) {
        while (startFormatting != null && startFormatting.getNode() != null &&
          !SYMMETRIC_FORMATTING.containsKey(startFormatting.getNode().getElementType()) &&
          !startFormatting.getText().contains("\n")) {
          startFormatting = startFormatting.getPrevSibling();
        }
        if (startFormatting == null) {
          break;
        }
        while (endFormatting != null && startFormatting.getNode() != null &&
          SYMMETRIC_FORMATTING.get(startFormatting.getNode().getElementType()) != endFormatting.getNode().getElementType() &&
          !endFormatting.getText().contains("\n")) {
          endFormatting = endFormatting.getNextSibling();
        }
        if (endFormatting == null) {
          break;
        }
        int startOffset = 0;
        if (startFormatting instanceof PsiWhiteSpace && startFormatting.getText().contains("\n")) {
          startOffset = startFormatting.getTextLength();
        }
        int endOffset = 0;
        if (endFormatting instanceof PsiWhiteSpace && endFormatting.getText().contains("\n")) {
          endOffset = -endFormatting.getTextLength() + endFormatting.getText().indexOf("\n");
        }
        if (startFormatting.getNode() != null && SYMMETRIC_FORMATTING.get(startFormatting.getNode().getElementType()) == endFormatting.getNode().getElementType()) {
          // we might be looking at the identical token (for example a double quote) for start and end,
          // prevent to report this with its end first and start second, which would lead to an inverse and icorrect range
          if (startFormatting.getTextRange().getEndOffset() < endFormatting.getTextRange().getStartOffset()) {
            ranges.add(TextRange.create(startFormatting.getTextRange().getEndOffset(), endFormatting.getTextRange().getStartOffset()));
          }
        }
        ranges.add(TextRange.create(startFormatting.getTextOffset() + startOffset, endFormatting.getTextRange().getEndOffset() + endOffset));
        // expand one step further and try again
        startFormatting = startFormatting.getPrevSibling();
        endFormatting = endFormatting.getNextSibling();
      }
      element = element.getParent();
    } while (element != null && !(element instanceof PsiFile));
    return ranges;
  }
}

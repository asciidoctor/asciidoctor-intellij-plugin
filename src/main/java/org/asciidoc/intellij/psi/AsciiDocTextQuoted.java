package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.BracePair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.braces.AsciiDocBraceMatcher;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BOLD_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.ITALIC_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.MONO_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_START;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START;

public class AsciiDocTextQuoted extends AsciiDocASTWrapperPsiElement {

  public static final Map<IElementType, IElementType> QUOTEPAIRS = new HashMap<>();
  public static final Set<IElementType> ALLQUOTES = new HashSet<>();

  static {
    for (BracePair pair : new AsciiDocBraceMatcher().getPairs()) {
      if (pair.getLeftBraceType() == BOLD_START ||
      pair.getLeftBraceType() == MONO_START  ||
      pair.getLeftBraceType() == ITALIC_START ||
      pair.getLeftBraceType() == TYPOGRAPHIC_SINGLE_QUOTE_START ||
      pair.getLeftBraceType() == TYPOGRAPHIC_DOUBLE_QUOTE_START) {
        QUOTEPAIRS.put(pair.getLeftBraceType(), pair.getRightBraceType());
        ALLQUOTES.add(pair.getLeftBraceType());
        ALLQUOTES.add(pair.getRightBraceType());
      }
    }
  }

  public AsciiDocTextQuoted(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocTextQuoted> {

    /**
     * This method will be called when the file is renamed.
     * <p>
     * Limitation: If the content change would spread children, renaming is not possible.
     */
    @Override
    public AsciiDocTextQuoted handleContentChange(@NotNull AsciiDocTextQuoted element,
                                                  @NotNull TextRange range,
                                                  String newContent) throws IncorrectOperationException {
      PsiElement child = element.getFirstChild();
      while (child != null && child.getStartOffsetInParent() < range.getStartOffset()) {
        child = child.getNextSibling();
      }
      if (child instanceof LeafPsiElement && child.getTextRangeInParent().equals(range)) {
        ((LeafPsiElement) child).replaceWithText(newContent);
      } else {
        throw new IncorrectOperationException("Can't change nested content");
      }
      return element;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull AsciiDocTextQuoted element) {
      return getBodyRange(element);
    }
  }

  public static TextRange getBodyRange(AsciiDocTextQuoted element) {
    PsiElement first = element.getFirstChild();
    PsiElement last = element.getLastChild();
    if (first == null || last == null) {
      return element.getTextRange();
    }
    while (QUOTEPAIRS.get(first.getNode().getElementType()) == last.getNode().getElementType()
     && first.getNode().getStartOffset() < last.getNode().getStartOffset()) {
      first = first.getNextSibling();
      last = last.getPrevSibling();
    }
    return new TextRange(first.getNode().getStartOffset(), last.getNode().getStartOffset() + last.getNode().getTextLength());
  }

  public boolean isItalic() {
    ASTNode node = getFirstChild().getNode();
    while (node != null && ALLQUOTES.contains(node.getElementType())) {
      if (node.getElementType() == AsciiDocTokenTypes.ITALIC_START) {
        return true;
      }
      node = node.getTreeNext();
    }
    return false;
  }

  public boolean isMono() {
    ASTNode node = getFirstChild().getNode();
    while (node != null && ALLQUOTES.contains(node.getElementType())) {
      if (node.getElementType() == AsciiDocTokenTypes.MONO_START) {
        return true;
      }
      node = node.getTreeNext();
    }
    return false;
  }

  public boolean isBold() {
    ASTNode node = getFirstChild().getNode();
    while (node != null && ALLQUOTES.contains(node.getElementType())) {
      if (node.getElementType() == BOLD_START) {
        return true;
      }
      node = node.getTreeNext();
    }
    return false;
  }

}

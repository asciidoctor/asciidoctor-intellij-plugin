package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.IncorrectOperationException;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractAsciiDocCodeBlock extends CompositePsiElement implements PsiLanguageInjectionHost, AsciiDocPsiElement, AsciiDocBlock, AsciiDocElementWithLanguage {
  AbstractAsciiDocCodeBlock(IElementType type) {
    super(type);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AsciiDocVisitor) {
      ((AsciiDocVisitor) visitor).visitBlocks(this);
      return;
    }

    super.accept(visitor);
  }

  @Override
  public Type getType() {
    return Type.LISTING;
  }

  @Override
  public String getDefaultTitle() {
    return "Listing";
  }

  @NotNull
  @Override
  public String getFoldedSummary() {
    PsiElement child = getFirstSignificantChildForFolding();
    if (child instanceof AsciiDocBlockAttributes) {
      return "[" + getStyle() + "]";
    }
    return child.getText();
  }

  @Override
  public ItemPresentation getPresentation() {
    return new ItemPresentation() {
      @Nullable
      @Override
      public String getPresentableText() {
        if (!isValid()) {
          return null;
        }
        return "Code fence";
      }

      @Nullable
      @Override
      public Icon getIcon(boolean unused) {
        return AsciiDocIcons.Structure.LISTING;
      }

      @Nullable
      @Override
      public String getLocationString() {
        if (!isValid()) {
          return null;
        }

        final StringBuilder sb = new StringBuilder();
        for (PsiElement child = getFirstChild(); child != null; child = child.getNextSibling()) {
          if (!(child instanceof AbstractAsciiDocCodeBlock)) {
            continue;
          }
          if (sb.length() > 0) {
            sb.append("\\n");
          }
          sb.append(child.getText());

          if (sb.length() >= AsciiDocCompositePsiElementBase.PRESENTABLE_TEXT_LENGTH) {
            break;
          }
        }

        return sb.toString();
      }
    };
  }

  @Override
  public boolean isValidHost() {
    // must not use PsiTreeUtil.findChildOfType as it leads to exception
    for (PsiElement e : this.getChildren()) {
      // if there is a block macro (typically an include), disable highlighting for all of it
      if (e instanceof AsciiDocBlockMacro) {
        return false;
      }
    }
    // check if there are i.e. non-matching elements
    return !getContentTextRange().equals(TextRange.EMPTY_RANGE);
  }

  /**
   * Replace complete text. Will be called when quick fixes in the fenced code occur.
   */
  @Override
  public PsiLanguageInjectionHost updateText(@NotNull String text) {
    return ElementManipulators.handleContentChange(this, text);
  }

  public TextRange getContentTextRange(IElementType... delimiter) {
    Set<IElementType> delimiters = new HashSet<>(Arrays.asList(delimiter));
    /*
     * The calculated text range needs to end with a newline. Otherwise the IDE will
     * give a strange behaviour when adding new lines at the end of fragment editing.
     */
    ASTNode node = this.getNode().getFirstChildNode();
    int offset = 0;
    while (node != null && !delimiters.contains(node.getElementType())) {
      offset += node.getTextLength();
      node = node.getTreeNext();
    }
    if (node == null) {
      return TextRange.EMPTY_RANGE;
    }
    IElementType myDelimiter = node.getElementType();
    if (myDelimiter == AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER && node.getText().startsWith("`")) {
      offset += node.getTextLength();
      node = node.getTreeNext();
      if (node == null) {
        return TextRange.EMPTY_RANGE;
      }
    }
    offset += node.getTextLength();
    node = node.getTreeNext();
    if (node == null) {
      return TextRange.EMPTY_RANGE;
    }
    offset += node.getTextLength();
    node = node.getTreeNext();
    int start = offset; // start will be after the delimiter and its newline
    while (node != null && node.getElementType() != myDelimiter) {
      offset += node.getTextLength();
      node = node.getTreeNext();
    }
    if (node == null) {
      return TextRange.EMPTY_RANGE;
    }
    int end = offset; // end will be just before the delimiter, but including the newline of the last code line
    return TextRange.create(start, end);
  }

  @NotNull
  @Override
  public LiteralTextEscaper<? extends AsciiDocElementWithLanguage> createLiteralTextEscaper() {
    return new LiteralTextEscaper<AsciiDocElementWithLanguage>(this) {
      @Override
      public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
        outChars.append(rangeInsideHost.substring(myHost.getText()));
        return true;
      }

      @Override
      public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
        return rangeInsideHost.getStartOffset() + offsetInDecoded;
      }

      @NotNull
      @Override
      public TextRange getRelevantTextRange() {
        return myHost.getContentTextRange();
      }

      @Override
      public boolean isOneLine() {
        return false;
      }
    };
  }

  @Override
  public abstract String getFenceLanguage();

  @Override
  public Icon getIcon(int ignored) {
    return AsciiDocIcons.Structure.LISTING;
  }

  public abstract static class AbstractManipulator<T extends AsciiDocElementWithLanguage> extends AbstractElementManipulator<T> {

    @Override
    @SuppressWarnings("unchecked")
    public T handleContentChange(@NotNull T element, @NotNull TextRange range, String newContent)
      throws IncorrectOperationException {
      if (newContent == null) {
        return null;
      }
      /* if the fenced content is edited, ensure that there is a newline at the end
       otherwise it will break the fencing */
      if (range.equals(element.createLiteralTextEscaper().getRelevantTextRange()) &&
        !StringUtil.endsWithLineBreak(newContent)) {
        newContent = newContent + "\n";
      }
      StringBuilder content = new StringBuilder(element.getText());
      content.replace(range.getStartOffset(), range.getEndOffset(), newContent);
      return (T) element.replace(createElement(element, content.toString()));
    }

    protected abstract T createElement(T element, String content);
  }

}

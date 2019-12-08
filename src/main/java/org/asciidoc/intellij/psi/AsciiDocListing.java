package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocListing extends AbstractAsciiDocCodeBlock {
  AsciiDocListing(IElementType type) {
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

  @Override
  public TextRange getContentTextRange() {
    return getContentTextRange(AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER);
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
  public String getFenceLanguage() {
    final PsiElement element = findPsiChildByType(AsciiDocElementTypes.BLOCK_ATTRIBUTES);
    if (element == null) {
      return null;
    }
    ASTNode[] attr = element.getNode().getChildren(TokenSet.create(AsciiDocTokenTypes.ATTR_NAME));
    if (attr.length >= 2 && "source".equalsIgnoreCase(attr[0].getText())) {
      return "source-" + attr[1].getText();
    } else if (attr.length >= 1 && "plantuml".equalsIgnoreCase(attr[0].getText())) {
      return "diagram-plantuml";
    } else if (attr.length >= 1 && "graphviz".equalsIgnoreCase(attr[0].getText())) {
      return "diagram-graphviz";
    }
    return null;
  }

  public static class Manipulator extends AbstractManipulator<AsciiDocListing> {

    @Override
    protected AsciiDocListing createElement(AsciiDocListing element, String content) {
      return AsciiDocPsiElementFactory.createListing(element.getProject(), content);
    }
  }

}

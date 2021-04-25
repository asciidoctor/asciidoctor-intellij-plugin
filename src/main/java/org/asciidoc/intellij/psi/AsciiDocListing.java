package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
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
    return getContentTextRange(AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER, AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER);
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
    // check for AsciiDoc block attributes
    final PsiElement element = findPsiChildByType(AsciiDocElementTypes.BLOCK_ATTRIBUTES);
    if (element != null) {
      AsciiDocBlockAttributes blockAttributes = PsiTreeUtil.findChildOfType(this, AsciiDocBlockAttributes.class);
      if (blockAttributes != null) {
        String[] attr = blockAttributes.getAttributes();
        String firstAttr = null;
        if (attr == null) {
          return null;
        } else if (attr.length >= 1) {
          firstAttr = attr[0];
          int locationOfPercent = firstAttr.indexOf("%"); // this handles for example "plantuml%interactive"
          if (locationOfPercent != -1) {
            firstAttr = firstAttr.substring(0, locationOfPercent);
          }
        }
        if (attr.length >= 2 && "source".equalsIgnoreCase(firstAttr)) {
          return "source-" + attr[1];
        } else if ("plantuml".equalsIgnoreCase(firstAttr)) {
          return "diagram-plantuml";
        } else if ("graphviz".equalsIgnoreCase(firstAttr)) {
          return "diagram-graphviz";
        }
      }
    }

    // check for markdown style listing
    PsiElement child = this.getFirstChild();
    if (child != null && child.getNode().getElementType() == AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER &&
        child.getText().startsWith("`")) {
      child = child.getNextSibling();
      if (child != null && child.getNode().getElementType() == AsciiDocTokenTypes.LISTING_TEXT) {
        return "source-" + child.getText();
      }
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

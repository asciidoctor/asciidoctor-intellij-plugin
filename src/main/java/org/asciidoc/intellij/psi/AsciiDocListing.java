package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
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
  public TextRange getContentTextRange() {
    return getContentTextRange(AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER, AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER);
  }

  @Override
  public boolean validateContent() {
    final PsiElement element = findPsiChildByType(AsciiDocElementTypes.BLOCK_ATTRIBUTES);
    if (element != null) {
      AsciiDocBlockAttributes blockAttributes = PsiTreeUtil.findChildOfType(this, AsciiDocBlockAttributes.class);
      if (blockAttributes != null) {
        String[] attr = blockAttributes.getAttributes();
        if (attr.length > 0 && attr[0].contains("%novalidate")) {
          return false;
        }
        String opts = blockAttributes.getAttribute("opts");
        if (opts != null) {
          for (String opt : opts.split(",")) {
            if (opt.trim().equals("novalidate")) {
              return false;
            }
          }
        }
      }
    }
    return true;
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
          int locationOfPercent = firstAttr.indexOf("%"); // this handles, for example, "plantuml%interactive"
          if (locationOfPercent != -1) {
            firstAttr = firstAttr.substring(0, locationOfPercent);
          }
        }
        if (attr.length >= 2 && ("source".equalsIgnoreCase(firstAttr) || firstAttr.isEmpty())) {
          // empty first attribute defaults to source
          return "source-" + attr[1];
        } else if ("plantuml".equalsIgnoreCase(firstAttr)) {
          return "diagram-plantuml";
        } else if ("salt".equalsIgnoreCase(firstAttr)) {
          return "diagram-salt";
        } else if ("graphviz".equalsIgnoreCase(firstAttr)) {
          return "diagram-graphviz";
        }
      }
    }

    // check for markdown style listing
    PsiElement child = this.getFirstChild();
    if (child != null && child.getNode() != null && child.getNode().getElementType() == AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER &&
        child.getText().startsWith("`")) {
      child = child.getNextSibling();
      if (child != null && child.getNode() != null && child.getNode().getElementType() == AsciiDocTokenTypes.LISTING_TEXT) {
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

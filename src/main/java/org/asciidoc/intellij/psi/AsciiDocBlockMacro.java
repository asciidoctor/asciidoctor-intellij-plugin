package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocBlockMacro extends AsciiDocBlock {
  public AsciiDocBlockMacro(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    if (getMacroName().equals("image") || getMacroName().equals("include")) {
      ASTNode bodyNode = getNode().findChildByType(AsciiDocTokenTypes.BLOCK_MACRO_BODY);
      if (bodyNode != null) {
        return new FileReferenceSet(bodyNode.getText(), this, bodyNode.getStartOffset() - getTextRange().getStartOffset(),
            null, false).getAllReferences();
      }
    }
    return super.getReferences();
  }

  public String getMacroName() {
    ASTNode idNode = getNode().findChildByType(AsciiDocTokenTypes.BLOCK_MACRO_ID);
    if (idNode == null) {
      throw new IllegalStateException("Parser failure: block macro without ID found: " + getText());
    }
    return StringUtil.trimEnd(idNode.getText(), "::");

  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocBlockMacro> {

    @Override
    public AsciiDocBlockMacro handleContentChange(@NotNull AsciiDocBlockMacro element,
                                                  @NotNull TextRange range,
                                                  String newContent) throws IncorrectOperationException {
      PsiElement child = element.getFirstChild();
      while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.BLOCK_MACRO_BODY) {
        range = range.shiftRight(-child.getTextLength());
        child = child.getNextSibling();
      }
      if (child instanceof LeafPsiElement) {
        ((LeafPsiElement) child).replaceWithText(range.replace(child.getText(), newContent));
      } else {
        throw new IncorrectOperationException("Bad child");
      }

      return element;
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull AsciiDocBlockMacro element) {
      final String text = element.findChildByType(AsciiDocTokenTypes.BLOCK_MACRO_BODY).getText();
      return TextRange.allOf(text);
    }
  }

}

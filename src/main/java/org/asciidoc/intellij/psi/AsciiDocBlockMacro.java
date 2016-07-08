package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
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
    if (getMacroName().equals("image")) {
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
}

package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocBlockAttributes extends ASTWrapperPsiElement {
  public AsciiDocBlockAttributes(@NotNull ASTNode node) {
    super(node);
  }

  public String getFirstPositionalAttribute() {
    ASTNode[] children = getNode().getChildren(TokenSet.create(AsciiDocTokenTypes.ATTR_NAME));
    if (children.length > 0) {
      return children[0].getText();
    }
    return null;
  }
}

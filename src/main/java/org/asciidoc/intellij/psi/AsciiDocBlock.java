package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class AsciiDocBlock extends ASTWrapperPsiElement {
  public AsciiDocBlock(@NotNull ASTNode node) {
    super(node);
  }

  @Nullable
  public String getTitle() {
    ASTNode titleNode = getNode().findChildByType(AsciiDocTokenTypes.TITLE);
    if (titleNode == null) {
      return null;
    }
    String text = titleNode.getText();
    return text.length() >= 1 ? text.substring(1) : "";
  }
}

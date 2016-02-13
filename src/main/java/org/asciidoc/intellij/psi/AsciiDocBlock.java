package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
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

  @Nullable
  public String getStyle() {
    AsciiDocBlockAttributes attrs = PsiTreeUtil.findChildOfType(this, AsciiDocBlockAttributes.class);
    if (attrs != null) {
      return attrs.getFirstPositionalAttribute();
    }
    return null;
  }
}

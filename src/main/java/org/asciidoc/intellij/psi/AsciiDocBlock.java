package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.Nullable;

public interface AsciiDocBlock extends PsiElement {

  @Nullable
  default String getTitle() {
    ASTNode titleNode = getNode().findChildByType(AsciiDocTokenTypes.TITLE);
    if (titleNode == null) {
      return null;
    }
    String text = titleNode.getText();
    return text.length() >= 1 ? text.substring(1) : "";
  }

  ASTNode getNode();

  @Nullable
  default String getStyle() {
    AsciiDocBlockAttributes attrs = PsiTreeUtil.findChildOfType(this, AsciiDocBlockAttributes.class);
    if (attrs != null) {
      return attrs.getFirstPositionalAttribute();
    }
    return null;
  }

}

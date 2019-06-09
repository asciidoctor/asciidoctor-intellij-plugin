package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.Nullable;

public interface AsciiDocBlock extends PsiElement, AsciiDocSelfDescribe {

  @Nullable
  default String getTitle() {
    ASTNode titleNode = getNode().findChildByType(AsciiDocTokenTypes.TITLE);
    if (titleNode == null) {
      return null;
    }
    String text = titleNode.getText();
    return text.length() >= 1 ? text.substring(1) : "";
  }

  @Override
  default String getDescription() {
    String title = getTitle();
    String style = getStyle();
    if (title == null) {
      title = "(Block)";
    } else {
      title = "";
    }
    if (style != null) {
      return "[" + style + "]" + (title.isEmpty() ? "" : " ") + title;
    }
    return title;
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

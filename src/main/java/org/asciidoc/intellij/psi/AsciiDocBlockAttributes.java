package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocBlockAttributes extends ASTWrapperPsiElement {
  public AsciiDocBlockAttributes(@NotNull ASTNode node) {
    super(node);
  }

  public String getFirstPositionalAttribute() {
    AsciiDocAttributeInBrackets child = PsiTreeUtil.findChildOfType(this, AsciiDocAttributeInBrackets.class);
    if (child != null) {
      return child.getAttrName();
    }
    return null;
  }
}

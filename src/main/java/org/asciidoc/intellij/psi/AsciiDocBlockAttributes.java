package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

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

  public String getAttribute(String name) {
    Collection<AsciiDocAttributeInBrackets> children = PsiTreeUtil.findChildrenOfType(this, AsciiDocAttributeInBrackets.class);
    for (AsciiDocAttributeInBrackets child : children) {
      if (child.getAttrName().equals(name)) {
        return child.getAttrValue();
      }
    }
    return null;
  }
}

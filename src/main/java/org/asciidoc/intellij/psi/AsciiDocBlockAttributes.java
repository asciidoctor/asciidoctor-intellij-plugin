package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yole
 */
public class AsciiDocBlockAttributes extends AsciiDocASTWrapperPsiElement {
  public AsciiDocBlockAttributes(@NotNull ASTNode node) {
    super(node);
  }

  public String getFirstPositionalAttribute() {
    String[] attributes = getAttributes();
    if (attributes != null && attributes.length > 0) {
      return attributes[0];
    }
    return null;
  }

  @Nullable
  public String[] getAttributes() {
    PsiElement firstChild = this.getFirstChild().getNextSibling();
    if (firstChild instanceof AsciiDocAttributeReference) {
      String resolved = AsciiDocUtil.resolveAttributes(this, firstChild.getText());
      if (resolved != null && !AsciiDocUtil.ATTRIBUTES.matcher(resolved).find()) {
        List<String> list = Arrays.stream(resolved.split(","))
          .map(s -> {
            s = s.trim();
            if (s.indexOf('=') != -1) {
              s = s.split("=", -1)[1].trim();
            }
            return s;
          })
          .collect(Collectors.toList());
        if (list.size() > 0) {
          return list.toArray(new String[]{});
        }
      }
    }

    Collection<AsciiDocAttributeInBrackets> children = PsiTreeUtil.findChildrenOfType(this, AsciiDocAttributeInBrackets.class);
    if (children.size() > 0) {
      return children.stream().map(AsciiDocAttributeInBrackets::getAttrName).toArray(String[]::new);
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

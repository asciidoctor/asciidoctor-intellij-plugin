package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    Collection<String> attrNames = new ArrayList<>();
    PsiElement child = this.getFirstChild();
    while (child != null) {
      if (child.getNode().getElementType() == AsciiDocTokenTypes.SEPARATOR) {
        // empty name attribute
        attrNames.add("");
        child = child.getNextSibling();
      } else if (child instanceof AsciiDocAttributeInBrackets aib) {
        attrNames.add(aib.getAttrName());
        child = child.getNextSibling();
        while (child != null) {
          // skip all following whitespaces and one separator
          if (child instanceof PsiWhiteSpace) {
            child = child.getNextSibling();
          } else if (child.getNode().getElementType() == AsciiDocTokenTypes.SEPARATOR) {
            child = child.getNextSibling();
            break;
          } else {
            // possibly ATTRS_END
            break;
          }
        }
      } else {
        child = child.getNextSibling();
      }
    }
    if (!attrNames.isEmpty()) {
      return attrNames.toArray(String[]::new);
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

  public AsciiDocBlockId getBlockId() {
    Collection<AsciiDocAttributeInBrackets> children = PsiTreeUtil.findChildrenOfType(this, AsciiDocAttributeInBrackets.class);
    for (AsciiDocAttributeInBrackets child : children) {
      AsciiDocBlockId blockId = PsiTreeUtil.findChildOfType(child, AsciiDocBlockId.class);
      if (blockId != null) {
        return blockId;
      }
    }
    return null;
  }
}

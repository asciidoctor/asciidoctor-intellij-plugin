package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocBlockAttributes extends ASTWrapperPsiElement {
  public AsciiDocBlockAttributes(@NotNull ASTNode node) {
    super(node);
  }

  public String getFirstPositionalAttribute() {
    // TODO Parse individual attributes
    return StringUtil.trimEnd(StringUtil.trimStart(getText(), "["), "]");
  }
}

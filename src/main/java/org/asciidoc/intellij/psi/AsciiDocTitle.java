package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class AsciiDocTitle extends ASTWrapperPsiElement {
  public AsciiDocTitle(@NotNull ASTNode node) {
    super(node);
  }

}

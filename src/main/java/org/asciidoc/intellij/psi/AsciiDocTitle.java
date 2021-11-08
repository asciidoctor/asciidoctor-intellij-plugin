package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class AsciiDocTitle extends AsciiDocASTWrapperPsiElement {
  public AsciiDocTitle(@NotNull ASTNode node) {
    super(node);
  }

}

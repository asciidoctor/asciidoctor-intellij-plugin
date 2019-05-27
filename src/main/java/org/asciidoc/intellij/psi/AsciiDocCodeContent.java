package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class AsciiDocCodeContent extends ASTWrapperPsiElement {
  public AsciiDocCodeContent(@NotNull ASTNode node) {
    super(node);
  }
}

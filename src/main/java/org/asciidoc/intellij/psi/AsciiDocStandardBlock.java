package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocStandardBlock extends ASTWrapperPsiElement implements AsciiDocBlock {
  public AsciiDocStandardBlock(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AsciiDocVisitor) {
      ((AsciiDocVisitor)visitor).visitBlocks(this);
      return;
    }

    super.accept(visitor);
  }

}

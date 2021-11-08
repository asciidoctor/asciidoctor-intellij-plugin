package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocList extends AsciiDocASTWrapperPsiElement implements AsciiDocBlock {
  public AsciiDocList(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AsciiDocVisitor) {
      ((AsciiDocVisitor) visitor).visitBlocks(this);
      return;
    }

    super.accept(visitor);
  }

  @Override
  public String getDefaultTitle() {
    return "List";
  }

  @NotNull
  @Override
  public String getFoldedSummary() {
    PsiElement child = getFirstSignificantChildForFolding();
    if (child instanceof AsciiDocBlockAttributes && getStyle() != null) {
      return "[" + getStyle() + "]";
    } else if (child != null) {
      return child.getText();
    } else {
      return "(List)";
    }
  }

  @Override
  public @Nullable String getTitle() {
    return AsciiDocBlock.super.getTitle();
  }

  @Override
  public Icon getIcon(int flags) {
    return AsciiDocIcons.Structure.LIST;
  }

}

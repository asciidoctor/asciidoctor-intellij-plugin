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
public class AsciiDocListItem extends AsciiDocASTWrapperPsiElement implements AsciiDocBlock {
  public AsciiDocListItem(@NotNull ASTNode node) {
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
    return "Item";
  }

  @NotNull
  @Override
  public String getFoldedSummary() {
    PsiElement child = getFirstSignificantChildForFolding();
    StringBuilder sb = new StringBuilder();
    if (child instanceof AsciiDocBlockAttributes) {
      sb.append("[").append(getStyle()).append("] ");
    }
    String summary = AsciiDocStandardBlock.summary(this);
    if (summary != null) {
      sb.append(summary);
    }
    if (sb.length() == 0) {
      sb.append("(").append(getDefaultTitle()).append(")");
    }
    return sb.toString();
  }

  @Override
  public @Nullable String getTitle() {
    return AsciiDocBlock.super.getTitle();
  }

  @Override
  public Icon getIcon(int flags) {
    return AsciiDocIcons.Structure.ITEM;
  }

}

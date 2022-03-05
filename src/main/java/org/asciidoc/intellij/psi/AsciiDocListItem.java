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

  @NotNull
  @Override
  public String getFoldedSummary() {
    PsiElement child = getFirstSignificantChildForFolding();
    StringBuilder sb = new StringBuilder();
    if (child instanceof AsciiDocBlockAttributes) {
      sb.append("[").append(getStyle()).append("] ");
    }
    return sb.append(AsciiDocStandardBlock.EXTRACTOR.summaryAsString(this)).toString();
  }

  @Override
  public @Nullable String getTitle() {
    String title = AsciiDocBlock.super.getTitle();
    if (title == null) {
      title = getFoldedSummary();
    }
    return title;
  }

  @Override
  public Icon getIcon(int flags) {
    return AsciiDocIcons.Structure.ITEM;
  }

}

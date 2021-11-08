package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Alexander Schwartz (alexander.schwartz@gmx.net)
 */
public class AsciiDocDescriptionItem extends AsciiDocASTWrapperPsiElement implements AsciiDocBlock {
  public AsciiDocDescriptionItem(@NotNull ASTNode node) {
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
    if (child instanceof AsciiDocBlockAttributes) {
      return "[" + getStyle() + "]";
    }
    if (child != null) {
      StringBuilder sb = new StringBuilder();
      while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.DESCRIPTION_END) {
        sb.append(child.getText());
        child = child.getNextSibling();
      }
      return sb.toString();
    } else {
      return "???";
    }
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

package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.TokenSet;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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
    return child.getText();
  }

  @NotNull
  @Override
  public String getDescription() {
    ASTNode delimiter = getNode().findChildByType(TokenSet.create(AsciiDocTokenTypes.BLOCK_DELIMITER,
      AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER, AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER));
    String title = getTitle();
    String style = getStyle();
    if (title == null) {
      if (delimiter != null && style == null) {
        String d = delimiter.getText();
        if (d.startsWith("|")) {
          title = "(Table)";
        } else if (d.startsWith("*")) {
          title = "(Sidebar)";
        } else if (d.startsWith("=")) {
          title = "(Example)";
        } else if (d.startsWith(".")) {
          title = "(Literal)";
        } else if (d.startsWith("+")) {
          title = "(Passthrough)";
        } else if (d.startsWith("_")) {
          title = "(Quote)";
        } else {
          title = "(Block)";
        }
      } else if (style == null) {
        title = "(Block)";
      } else {
        title = "";
      }
    }
    if (style != null) {
      return "[" + style + "]" + (title.isEmpty() ? "" : " ") + title;
    }
    return title;
  }

  @Override
  public Type getType() {
    Type type = Type.UNKNOWN;
    ASTNode delimiter = getNode().findChildByType(TokenSet.create(AsciiDocTokenTypes.BLOCK_DELIMITER,
      AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER, AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER));
    if (delimiter != null) {
      String d = delimiter.getText();
      if (d.startsWith("|")) {
        type = Type.TABLE;
      } else if (d.startsWith("*")) {
        type = Type.SIDEBAR;
      } else if (d.startsWith("=")) {
        type = Type.EXAMPLE;
      } else if (d.startsWith(".")) {
        type = Type.LITERAL;
      } else if (d.startsWith("+")) {
        type = Type.PASSTHROUGH;
      } else if (d.startsWith("_")) {
        if ("verse".equals(getStyle())) {
          type = Type.VERSE;
        } else {
          type = Type.QUOTE;
        }
      }
    }
    return type;
  }

  @Override
  public Icon getIcon(int flags) {
    return AsciiDocIcons.Structure.BLOCK;
  }

}

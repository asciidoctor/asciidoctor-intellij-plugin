package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.ControlFlowException;
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.TokenSet;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.grazie.AsciiDocGrazieTextExtractor;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author yole
 */
public class AsciiDocStandardBlock extends AsciiDocASTWrapperPsiElement implements AsciiDocBlock {
  private static AsciiDocGrazieTextExtractor extractor = null;

  static {
    try {
      // Grazie plugin might not be installed. Gracefully fallback if that is the case.
      Class.forName("com.intellij.grazie.text.TextExtractor");
      extractor = new AsciiDocGrazieTextExtractor();
    } catch (ClassNotFoundException e) {
      // NOOP
    }
  }

  protected static String summary(PsiElement element) {
    if (extractor != null) {
      return extractor.summaryAsString(element);
    } else {
      return element.getText().trim();
    }
  }

  public AsciiDocStandardBlock(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AsciiDocVisitor) {
      ((AsciiDocVisitor) visitor).visitBlocks(this);
      return;
    }

    try {
      super.accept(visitor);
    } catch (RuntimeException e) {
      if (e instanceof RuntimeExceptionWithAttachments || e instanceof ControlFlowException) {
        throw e;
      }
      throw AsciiDocPsiImplUtil.getRuntimeException("Problem occurred while running visitor " + visitor.getClass(), this, e);
    }
  }

  @NotNull
  @Override
  public String getFoldedSummary() {
    String title = getTitle();
    if (title != null) {
      return title;
    }
    StringBuilder sb = new StringBuilder();
    if (!getDefaultTitle().equals("Table")) {
      PsiElement child = getFirstSignificantChildForFolding();
      if (child instanceof AsciiDocBlockAttributes && !StringUtil.isEmpty(getStyle())) {
        sb.append("[").append(getStyle()).append("] ");
      }
      String summary = AsciiDocStandardBlock.summary(this);
      if (summary != null) {
        sb.append(summary);
      }
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
  @SuppressWarnings("checkstyle:MissingSwitchDefault")
  public String getDefaultTitle() {
    String title = null;
    switch (getType()) {
      case UNKNOWN:
        break;
      case TABLE:
        title = "Table";
        break;
      case SIDEBAR:
        title = "Sidebar";
        break;
      case EXAMPLE:
        title = "Example";
        break;
      case LITERAL:
        title = "Literal";
        break;
      case PASSTHROUGH:
        title = "Passthrough";
        break;
      case QUOTE:
        title = "Quote";
        break;
      case BLOCKMACRO:
        title = "Blockmacro";
        break;
      case LISTING:
        title = "Listing";
        break;
      case VERSE:
        title = "Verse";
        break;
      case FRONTMATTER:
        title = "Frontmatter";
        break;
    }
    if (title == null) {
      title = AsciiDocBlock.super.getDefaultTitle();
    }
    return title;
  }

  @Override
  public Type getType() {
    Type type = Type.UNKNOWN;
    ASTNode delimiter = getNode().findChildByType(TokenSet.create(AsciiDocTokenTypes.BLOCK_DELIMITER, AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER));
    if (delimiter != null) {
      String d = delimiter.getText();
      if (d.startsWith("|") || d.startsWith(";") || d.startsWith(":") || d.startsWith("!")) {
        type = Type.TABLE;
      } else if (d.startsWith("*")) {
        type = Type.SIDEBAR;
      } else if (d.startsWith("=")) {
        type = Type.EXAMPLE;
      } else if (d.startsWith(".")) {
        type = Type.LITERAL;
      } else if (d.startsWith("_")) {
        if ("verse".equals(getStyle())) {
          type = Type.VERSE;
        } else {
          type = Type.QUOTE;
        }
      } else if (d.startsWith("-")) {
        if ("verse".equals(getStyle())) {
          type = Type.VERSE;
        }
      }
    } else {
      if ("verse".equals(getStyle())) {
        type = Type.VERSE;
      }
    }
    return type;
  }

  @Override
  public Icon getIcon(int flags) {
    return AsciiDocIcons.Structure.BLOCK;
  }

}

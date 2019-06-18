package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.IncorrectOperationException;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

public class AsciiDocListing extends CompositePsiElement implements PsiLanguageInjectionHost, AsciiDocPsiElement, AsciiDocBlock {
  public AsciiDocListing(IElementType type) {
    super(type);
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
  public String getDescription() {
    String title = getTitle();
    String style = getStyle();
    if (title == null) {
      if (style == null) {
        title = "(Listing)";
      } else {
        title = "";
      }
    }
    if (style != null) {
      return "[" + style + "]" + (title.isEmpty() ? "" : " ") + title;
    }
    return title;
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

  @Override
  public ItemPresentation getPresentation() {
    return new ItemPresentation() {
      @Nullable
      @Override
      public String getPresentableText() {
        if (!isValid()) {
          return null;
        }
        return "Code fence";
      }

      @Nullable
      @Override
      public Icon getIcon(boolean unused) {
        return AsciiDocIcons.Structure.LISTING;
      }

      @Nullable
      @Override
      public String getLocationString() {
        if (!isValid()) {
          return null;
        }

        final StringBuilder sb = new StringBuilder();
        for (PsiElement child = getFirstChild(); child != null; child = child.getNextSibling()) {
          if (!(child instanceof AsciiDocListing)) {
            continue;
          }
          if (sb.length() > 0) {
            sb.append("\\n");
          }
          sb.append(child.getText());

          if (sb.length() >= AsciiDocCompositePsiElementBase.PRESENTABLE_TEXT_LENGTH) {
            break;
          }
        }

        return sb.toString();
      }
    };
  }

  @Override
  public boolean isValidHost() {
    return true;
  }

  @Override
  public PsiLanguageInjectionHost updateText(@NotNull String text) {
    return ElementManipulators.handleContentChange(this, text);
  }

  public static TextRange getContentTextRange(PsiElement myHost) {
    // must not use PsiTreeUtil.findChildOfType as it leads to exception
    for (PsiElement e : myHost.getChildren()) {
      if (e instanceof AsciiDocCodeContent) {
        for (PsiElement i : e.getChildren()) {
          // if there is a block macro (typically an include), disable highlighting for all of it
          if (i instanceof AsciiDocBlockMacro) {
            return TextRange.EMPTY_RANGE;
          }
        }
        return TextRange.create(e.getStartOffsetInParent(), e.getStartOffsetInParent() + e.getTextLength());
      }
    }
    return TextRange.EMPTY_RANGE;
  }

  @NotNull
  @Override
  public LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
    return new LiteralTextEscaper<PsiLanguageInjectionHost>(this) {
      @Override
      public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
        outChars.append(rangeInsideHost.substring(myHost.getText()));
        return true;
      }

      @Override
      public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
        return rangeInsideHost.getStartOffset() + offsetInDecoded;
      }

      @NotNull
      @Override
      public TextRange getRelevantTextRange() {
        return getContentTextRange(myHost);
      }

      @Override
      public boolean isOneLine() {
        return false;
      }
    };
  }

  public String getFenceLanguage() {
    final PsiElement element = findPsiChildByType(AsciiDocElementTypes.BLOCK_ATTRIBUTES);
    if (element == null) {
      return null;
    }
    ASTNode[] attr = element.getNode().getChildren(TokenSet.create(AsciiDocTokenTypes.BLOCK_ATTR_NAME));
    if (attr.length >= 2 && "source".equalsIgnoreCase(attr[0].getText())) {
      return "source-" + attr[1].getText();
    } else if (attr.length >= 1 && "plantuml".equalsIgnoreCase(attr[0].getText())) {
      return "diagram-plantuml";
    } else if (attr.length >= 1 && "graphviz".equalsIgnoreCase(attr[0].getText())) {
      return "diagram-graphviz";
    }
    return null;
  }

  @Override
  public Icon getIcon(int ignored) {
    return AsciiDocIcons.Structure.LISTING;
  }

  public static class Manipulator extends AbstractElementManipulator<AsciiDocListing> {

    @Override
    public AsciiDocListing handleContentChange(@NotNull AsciiDocListing element, @NotNull TextRange range, String newContent)
      throws IncorrectOperationException {
      if (newContent == null) {
        return null;
      }

      return (AsciiDocListing) element.replace(AsciiDocPsiElementFactory.createListing(element.getProject(), Objects.requireNonNull(newContent)));
    }
  }

}

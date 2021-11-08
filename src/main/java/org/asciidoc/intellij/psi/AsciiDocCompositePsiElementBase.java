package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import icons.AsciiDocIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public abstract class AsciiDocCompositePsiElementBase extends AsciiDocASTWrapperPsiElement implements AsciiDocCompositePsiElement {
  public static final int PRESENTABLE_TEXT_LENGTH = 50;

  protected AsciiDocCompositePsiElementBase(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  protected CharSequence getChars() {
    return getTextRange().subSequence(getContainingFile().getViewProvider().getContents());
  }

  @NotNull
  protected String shrinkTextTo(int length) {
    final CharSequence chars = getChars();
    return chars.subSequence(0, Math.min(length, chars.length())).toString();
  }

  @NotNull
  @Override
  public List<AsciiDocPsiElement> getCompositeChildren() {
    return Arrays.asList(findChildrenByClass(AsciiDocPsiElement.class));
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
        return getPresentableTagName();
      }

      @Nullable
      @Override
      public Icon getIcon(boolean unused) {
        return AsciiDocIcons.ASCIIDOC_ICON;
      }

      @Nullable
      @Override
      public String getLocationString() {
        if (!isValid()) {
          return null;
        }
        if (getCompositeChildren().size() == 0) {
          return shrinkTextTo(PRESENTABLE_TEXT_LENGTH);
        } else {
          return null;
        }
      }
    };
  }
}

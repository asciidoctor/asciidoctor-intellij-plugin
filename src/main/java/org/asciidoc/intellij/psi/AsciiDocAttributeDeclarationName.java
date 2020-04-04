package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.util.IncorrectOperationException;
import icons.AsciiDocIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Locale;
import java.util.Objects;

public class AsciiDocAttributeDeclarationName extends ASTWrapperPsiElement implements AsciiDocNamedElement {
  public AsciiDocAttributeDeclarationName(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public boolean isEquivalentTo(PsiElement another) {
    if (!(another instanceof AsciiDocAttributeDeclarationName)) {
      return false;
    }
    String myName = getName();
    String otherName = ((AsciiDocAttributeDeclarationName) another).getName();
    if (myName == null && otherName == null) {
      return true;
    }
    if (myName == null || otherName == null) {
      return false;
    }
    if (Objects.equals(myName.toLowerCase(Locale.US), otherName.toLowerCase(Locale.US))) {
      return true;
    }
    return false;
  }

  @Nullable
  @Override
  public PsiElement getNameIdentifier() {
    ASTNode keyNode = this.getNode();
    return keyNode.getPsi();
  }

  @Override
  public String getName() {
    ASTNode keyNode = this.getNode();
    return keyNode.getText().replaceAll("[ \t]", "");
  }

  @Override
  public Icon getIcon(int ignored) {
    return AsciiDocIcons.Structure.ATTRIBUTE;
  }

  @Override
  public PsiElement setName(@NotNull String s) throws IncorrectOperationException {
    ASTNode node = getNode().getFirstChildNode();
    if (node instanceof LeafElement) {
      ((LeafElement) node).replaceWithText(s);
    } else {
      throw new IncorrectOperationException("Bad child");
    }
    return this;
  }

  public String getId() {
    return getName();
  }

  @Override
  public ItemPresentation getPresentation() {
    return super.getPresentation();
  }
}

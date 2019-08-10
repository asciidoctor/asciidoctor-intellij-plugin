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

public class AsciiDocAttributeDeclarationName extends ASTWrapperPsiElement implements AsciiDocNamedElement {
  public AsciiDocAttributeDeclarationName(@NotNull ASTNode node) {
    super(node);
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
    return keyNode.getText();
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

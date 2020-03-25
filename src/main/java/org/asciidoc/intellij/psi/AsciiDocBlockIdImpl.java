package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import org.asciidoc.intellij.namesValidator.AsciiDocRenameInputValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocBlockIdImpl extends AsciiDocBlockIdStubElementImpl<AsciiDocBlockIdStub> implements AsciiDocBlockId, AsciiDocNamedElement {

  public AsciiDocBlockIdImpl(AsciiDocBlockIdStub stub, IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public AsciiDocBlockIdImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Nullable
  @Override
  public PsiElement getNameIdentifier() {
    ASTNode keyNode = this.getNode();
    return keyNode.getPsi();
  }

  @Override
  @NotNull
  public String getName() {
    final AsciiDocBlockIdStub stub = getGreenStub();
    if (stub != null) {
      return stub.getName();
    }
    ASTNode keyNode = this.getNode();
    return keyNode.getText();
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

  @Override
  public ItemPresentation getPresentation() {
    return super.getPresentation();
  }

  @Override
  public boolean patternIsValid() {
    return AsciiDocRenameInputValidator.BLOCK_ID_PATTERN.matcher(getName()).matches();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + getNode().getElementType().toString() + ")";
  }
}

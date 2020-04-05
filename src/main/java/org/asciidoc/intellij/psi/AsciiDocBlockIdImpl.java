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

import java.util.Objects;

public class AsciiDocBlockIdImpl extends AsciiDocBlockIdStubElementImpl<AsciiDocBlockIdStub> implements AsciiDocBlockId, AsciiDocNamedElement {

  public AsciiDocBlockIdImpl(AsciiDocBlockIdStub stub, IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public AsciiDocBlockIdImpl(@NotNull ASTNode node) {
    super(node);
  }

  /**
   * Two elements are equivalent if the carry the same name and have been defined in the same file.
   */
  @Override
  public boolean isEquivalentTo(PsiElement another) {
    if (!(another instanceof AsciiDocBlockId)) {
      return false;
    }
    String name1 = this.getName();
    String name2 = ((AsciiDocBlockId) another).getName();
    if (!name1.equals(name2)) {
      return false;
    }
    if (!Objects.equals(this.getContainingFile().getVirtualFile(), another.getContainingFile().getVirtualFile())) {
      return false;
    }
    return true;
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
      ASTNode next = node.getTreeNext();
      // this is a brute-force-approach to drop all other nodes except the first one in the case there are attribute names
      while (next != null) {
        next.getTreeParent().removeChild(next);
        next = node.getTreeNext();
      }
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

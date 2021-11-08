package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * This extends the base class with a modification stamp counter.
 */
public abstract class AsciiDocASTWrapperPsiElement extends ASTWrapperPsiElement implements AsciiDocModificationTracker {
  private long myModificationStamp;

  public AsciiDocASTWrapperPsiElement(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void subtreeChanged() {
    ++myModificationStamp;
    super.subtreeChanged();
  }

  @Override
  public long getModificationCount() {
    return myModificationStamp;
  }
}

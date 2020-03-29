package org.asciidoc.intellij.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;

public interface HasAnchorReference {
  @Nullable
  default AsciiDocFileReference getAnchorReference() {
    for (PsiReference reference : getReferences()) {
      if (reference instanceof AsciiDocFileReference) {
        if (((AsciiDocFileReference) reference).isAnchor()) {
          return (AsciiDocFileReference) reference;
        }
      }
    }
    return null;
  }

  @Nullable
  default AsciiDocSection resolveAnchorForSection() {
    AsciiDocFileReference anchor = getAnchorReference();
    if (anchor != null) {
      PsiElement resolve = anchor.resolve();
      if (resolve instanceof AsciiDocFile) {
        resolve = resolve.getFirstChild();
      }
      if (resolve instanceof AsciiDocSection) {
        return (AsciiDocSection) resolve;
      }
    }
    return null;
  }

  PsiReference[] getReferences();

}

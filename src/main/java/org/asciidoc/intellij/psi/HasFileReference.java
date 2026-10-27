package org.asciidoc.intellij.psi;

import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;

public interface HasFileReference {

  @Nullable
  default AsciiDocFileReference getFileReference() {
    for (PsiReference reference : getReferences()) {
      if (reference instanceof AsciiDocFileReference fileReference) {
        
        if (!fileReference.isFolder() && !fileReference.isAnchor()) {
          return fileReference;
        }
      }
    }
    return null;
  }

  PsiReference[] getReferences();

}

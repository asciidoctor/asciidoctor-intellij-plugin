package org.asciidoc.intellij.psi;

import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;

public interface HasAntoraReference {

  @Nullable
  default AsciiDocFileReference getAntoraReference() {
    for (PsiReference reference : getReferences()) {
      if (reference instanceof AsciiDocFileReference fileReference) {
        
        if (fileReference.isAntora()) {
          return fileReference;
        }
      }
    }
    return null;
  }

  PsiReference[] getReferences();

}

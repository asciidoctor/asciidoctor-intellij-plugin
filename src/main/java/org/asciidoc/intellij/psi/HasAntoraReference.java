package org.asciidoc.intellij.psi;

import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;

public interface HasAntoraReference {

  @Nullable
  default AsciiDocFileReference getAntoraReference() {
    for (PsiReference reference : getReferences()) {
      if (reference instanceof AsciiDocFileReference) {
        AsciiDocFileReference fileReference = (AsciiDocFileReference) reference;
        if (fileReference.isAntora()) {
          return (AsciiDocFileReference) reference;
        }
      }
    }
    return null;
  }

  PsiReference[] getReferences();

}

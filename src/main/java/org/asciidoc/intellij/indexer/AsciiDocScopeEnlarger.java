package org.asciidoc.intellij.indexer;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UseScopeEnlarger;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocScopeEnlarger extends UseScopeEnlarger {
  @Nullable
  @Override
  public SearchScope getAdditionalUseScope(@NotNull PsiElement element) {
    // any element can be referenced from an AsciiDoc element within the same project (directory, file, image, or an ID in an AsciiDoc file)
    if (element.getLanguage() == AsciiDocLanguage.INSTANCE || element instanceof PsiFile || element instanceof PsiDirectory) {
      return new AsciiDocSearchScope(element.getProject()).restrictedByAsciiDocFileType();
    }
    return null;
  }
}

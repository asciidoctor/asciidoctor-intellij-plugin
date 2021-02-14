package org.asciidoc.intellij.indexer;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UseScopeEnlarger;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocScopeEnlarger extends UseScopeEnlarger {
  @Nullable
  @Override
  public SearchScope getAdditionalUseScope(@NotNull PsiElement element) {
    // not restricting to GlobalSearchScope breaks refactorings like extract-variable
    if (element.getUseScope() instanceof GlobalSearchScope) {
      // any element can be referenced from an AsciiDoc element within the same project (directory, file, image, or an ID in an AsciiDoc file)
      return GlobalSearchScope.getScopeRestrictedByFileTypes(new AsciiDocSearchScope(element.getProject()), AsciiDocFileType.INSTANCE);
    }
    return null;
  }
}

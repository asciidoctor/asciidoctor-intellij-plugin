package org.asciidoc.intellij.indexer;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UseScopeEnlarger;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocScopeEnlarger extends UseScopeEnlarger {
  @Nullable
  @Override
  public SearchScope getAdditionalUseScope(@NotNull PsiElement element) {
    if (element.getLanguage() == AsciiDocLanguage.INSTANCE) {
      return GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.projectScope(element.getProject()), AsciiDocFileType.INSTANCE);
    }
    return null;
  }
}

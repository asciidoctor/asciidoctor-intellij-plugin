package org.asciidoc.intellij.indexer;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UseScopeEnlarger;
import com.intellij.util.SlowOperations;
import org.asciidoc.intellij.psi.AsciiDocSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocScopeEnlarger extends UseScopeEnlarger {
  @Nullable
  @Override
  public SearchScope getAdditionalUseScope(@NotNull PsiElement element) {
    // not restricting to GlobalSearchScope breaks refactorings like extract-variable
    // Leads to EDT problems with EAP 2024.1.
    // Allowing via SlowOperations.allowSlowOperations() as a workaround in
    // https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/1533
    // doesn't work as expected. The original cause for this check as described in https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/466
    // seems no longer present - therefore removing the check
    // if (element.getUseScope() instanceof GlobalSearchScope) {
    // any element can be referenced from an AsciiDoc element within the same project (directory, file, image, or an ID in an AsciiDoc file)
    return new AsciiDocSearchScope(element.getProject()).restrictedByAsciiDocFileType();
    // }
    // return null;
  }
}

package org.asciidoc.intellij.indexer;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UseScopeEnlarger;
import org.asciidoc.intellij.psi.AsciiDocSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocJavaScopeEnlarger extends UseScopeEnlarger {
  @Nullable
  @Override
  public SearchScope getAdditionalUseScope(@NotNull PsiElement element) {
    // Any Java class or package can be referenced from an AsciiDoc element within the same project.
    // See org.asciidoc.intellij.psi.AsciiDocJavaReference for the code. Both have the com.intellij.psi.search.GlobalSearchScope, as otherwise this might break refactorings
    // as previously reported in https://github.com/asciidoctor/asciidoctor-intellij-plugin/issues/1591
    if (element instanceof PsiClass || element instanceof PsiPackage) {
      return new AsciiDocSearchScope(element.getProject()).restrictedByAsciiDocFileType();
    }
    return null;
  }
}

package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.impl.search.LowLevelSearchUtil;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import com.intellij.util.text.StringSearcher;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocJavaReference;
import org.jetbrains.annotations.NotNull;

public class AsciiDocJavaReferencesSearch extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  protected AsciiDocJavaReferencesSearch() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters p, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement element = p.getElementToSearch();
    if (!element.isValid()) {
      return;
    }

    final SearchScope scope = p.getEffectiveSearchScope();
    // this will limit the search to Java files that are recognized as part of a project
    // otherwise it will return "LocalScope" here
    if (!(scope instanceof GlobalSearchScope)) {
      return;
    }

    if (element instanceof PsiClass) {
      String name = ((PsiClass) element).getName();
      if (name != null) {
        search(consumer, element, name);
      }
    } else if (element instanceof PsiPackage) {
      String name = ((PsiPackage) element).getName();
      if (name != null) {
        search(consumer, element, name);
      }
    }
  }

  private void search(@NotNull Processor<? super PsiReference> consumer, PsiElement element, String name) {
    DumbService myDumbService = DumbService.getInstance(element.getProject());
    final StringSearcher searcher = new StringSearcher(name, true, true, false);
    PsiFile[] files = myDumbService.runReadActionInSmartMode(() -> CacheManager.SERVICE.getInstance(element.getProject()).getFilesWithWord(name, UsageSearchContext.IN_CODE,
      // don't look in module scope and modules that depend on this module, but in all project scope
      // as documentation might reference classes in other modules
      GlobalSearchScope.projectScope(element.getProject()),
      false));
    for (PsiFile psiFile : files) {
      if (psiFile.getLanguage() == AsciiDocLanguage.INSTANCE) {
        final CharSequence text = ReadAction.compute(() -> psiFile.getViewProvider().getContents());
        LowLevelSearchUtil.processTextOccurrences(text, 0, text.length(), searcher, null, index -> {
          myDumbService.runReadActionInSmartMode(() -> {
            PsiReference referenceAt = psiFile.findReferenceAt(index);
            if (referenceAt instanceof PsiMultiReference) {
              for (PsiReference reference : ((PsiMultiReference) referenceAt).getReferences()) {
                checkReference(consumer, element, reference);
              }
            }
            checkReference(consumer, element, referenceAt);
          });
          return true;
        });
      }
    }
  }

  private void checkReference(@NotNull Processor<? super PsiReference> consumer, PsiElement element, PsiReference reference) {
    if (reference instanceof AsciiDocJavaReference) {
      AsciiDocJavaReference javaReference = (AsciiDocJavaReference) reference;
      if (javaReference.matches(element)) {
        consumer.process(reference);
      }
    }
  }

}

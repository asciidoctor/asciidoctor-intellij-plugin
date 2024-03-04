package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.impl.search.LowLevelSearchUtil;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import com.intellij.util.text.StringSearcher;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocJavaReference;
import org.asciidoc.intellij.threading.AsciiDocProcessUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocJavaReferencesSearch extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  protected AsciiDocJavaReferencesSearch() {
    super(false);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters p, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement element = p.getElementToSearch();
    if (!(element instanceof PsiClass) && !(element instanceof PsiPackage)) {
      return;
    }
    AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> {
      if (!element.isValid()) {
        return;
      }

      // use scope determined by user here, as effective search scope would return the module and its dependants
      SearchScope scope = p.getScopeDeterminedByUser();

      if (element instanceof PsiClass) {
        String name = ((PsiClass) element).getName();
        if (name != null && !name.isEmpty()) {
          search(consumer, element, name, scope);
        }
      } else //noinspection ConstantConditions
        if (element instanceof PsiPackage) {
        String name = ((PsiPackage) element).getName();
        if (name != null && !name.isEmpty()) {
          search(consumer, element, name, scope);
        }
      }
    });
  }

  private void search(@NotNull Processor<? super PsiReference> consumer, PsiElement element, String name, SearchScope scope) {
    DumbService myDumbService = DumbService.getInstance(element.getProject());
    PsiFile[] files;
    if (scope instanceof GlobalSearchScope) {
      // when the user searches all references
      files = myDumbService.runReadActionInSmartMode(() ->
        CacheManager.getInstance(element.getProject())
          .getFilesWithWord(name, UsageSearchContext.IN_CODE, GlobalSearchScope.getScopeRestrictedByFileTypes((GlobalSearchScope) scope, AsciiDocFileType.INSTANCE), false));
      if (files.length == 0) {
        return;
      }
    } else if (scope instanceof LocalSearchScope localSearchScope) {
      // when the IDE highlights references of the current file in the editor
      if (localSearchScope.getScope().length == 1 && localSearchScope.getScope()[0] instanceof PsiFile) {
        files = new PsiFile[] {(PsiFile) localSearchScope.getScope()[0]};
      } else {
        return;
      }
    } else {
      return;
    }
    final StringSearcher searcher = new StringSearcher(name, true, true, false);
    @Nullable ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
    if (pi != null) {
      pi.setText("Searching text in " + files.length + " files");
      pi.setIndeterminate(false);
    }
    for (int i = 0; i < files.length; i++) {
      PsiFile psiFile = files[i];
      if (pi != null) {
        pi.setFraction((double) i / files.length);
        VirtualFile vf = psiFile.getVirtualFile();
        if (vf != null) {
          pi.setText2(vf.getPresentableName());
        } else {
          pi.setText2(null);
        }
      }
      ProgressManager.checkCanceled();
      final CharSequence text = ReadAction.compute(psiFile::getText);
      LowLevelSearchUtil.processTexts(text, 0, text.length(), searcher, index -> {
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
    if (pi != null) {
      pi.setText2(null);
    }
  }

  private void checkReference(@NotNull Processor<? super PsiReference> consumer, PsiElement element, PsiReference reference) {
    if (reference instanceof AsciiDocJavaReference javaReference) {
      if (javaReference.matches(element)) {
        consumer.process(reference);
      }
    }
  }

}

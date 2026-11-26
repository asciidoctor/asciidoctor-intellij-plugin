package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.HasFileReference;
import org.asciidoc.intellij.threading.AsciiDocProcessUtil;
import org.jetbrains.annotations.NotNull;

public class AsciiDocFileReferencesSearch extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  protected AsciiDocFileReferencesSearch() {
    super(false);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters parameters,
                           @NotNull Processor<? super PsiReference> consumer) {
    PsiElement target = parameters.getElementToSearch();
    if (!(target instanceof PsiFile) && !(target instanceof PsiDirectory)) {
      return;
    }
    AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> {
      if (!target.isValid()) {
        return;
      }
      SearchScope scope = parameters.getEffectiveSearchScope();
      if (scope instanceof GlobalSearchScope globalScope) {
        GlobalSearchScope projectScope = globalScope.intersectWith(GlobalSearchScope.projectScope(target.getProject()));
        for (VirtualFile file : ReadAction.nonBlocking(() ->
          FileTypeIndex.getFiles(AsciiDocFileType.INSTANCE, projectScope))
          .inSmartMode(target.getProject()).executeSynchronously()) {
          ProgressManager.checkCanceled();
          boolean proceed = ReadAction.nonBlocking(() -> {
            PsiFile psiFile = PsiManager.getInstance(target.getProject()).findFile(file);
            return psiFile == null || search(psiFile, target, consumer);
          }).inSmartMode(target.getProject()).executeSynchronously();
          if (!proceed) {
            return;
          }
        }
      } else if (scope instanceof LocalSearchScope localScope) {
        for (PsiElement root : localScope.getScope()) {
          PsiFile file = root.getContainingFile();
          if (file != null && file.getFileType() == AsciiDocFileType.INSTANCE && !search(root, target, consumer)) {
            return;
          }
        }
      }
    });
  }

  private boolean search(PsiElement root, PsiElement target, Processor<? super PsiReference> consumer) {
    if (!root.textContains('{')) {
      return true;
    }
    return PsiTreeUtil.processElements(root, element -> {
      ProgressManager.checkCanceled();
      if (element instanceof HasFileReference && element.textContains('{')) {
        for (PsiReference reference : element.getReferences()) {
          if (reference instanceof AsciiDocFileReference fileReference) {
            for (ResolveResult result : fileReference.multiResolve(false)) {
              if (target.getManager().areElementsEquivalent(target, result.getElement())) {
                if (!consumer.process(reference)) {
                  return false;
                }
                break;
              }
            }
          }
        }
      }
      return true;
    });
  }
}

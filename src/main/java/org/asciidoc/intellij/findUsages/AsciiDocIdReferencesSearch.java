package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.impl.search.LowLevelSearchUtil;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;
import com.intellij.util.text.StringSearcher;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationKeyIndex;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationName;
import org.asciidoc.intellij.psi.AsciiDocBlockId;
import org.asciidoc.intellij.psi.AsciiDocNamedElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Locale;

public class AsciiDocIdReferencesSearch extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  protected AsciiDocIdReferencesSearch() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters p, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement element = p.getElementToSearch();
    if (!element.isValid()) {
      return;
    }

    // use scope determined by user here, as effective search scope would return the module and its dependants
    SearchScope scope = p.getScopeDeterminedByUser();

    if (element instanceof AsciiDocNamedElement) {
      String name = ((AsciiDocNamedElement) element).getName();
      if (name != null) {
        search(consumer, element, name, scope);
      }
    }
  }

  private void search(@NotNull Processor<? super PsiReference> consumer, PsiElement element, String name, SearchScope scope) {
    DumbService myDumbService = DumbService.getInstance(element.getProject());
    PsiFile[] files;
    boolean localSearch = false;
    if (scope instanceof GlobalSearchScope) {
      // when the user searches all references
      String nameToSearch = name;
      if (element instanceof AsciiDocBlockId) {
        // AsciiDoc block IDs contain attributes, these won't be in the index as words, therefore search only for the start part (or the attribute name)
        int attributeStart = name.indexOf('{');
        int attributeEnd = name.indexOf('}', attributeStart);
        if (attributeStart > 0) {
          nameToSearch = name.substring(0, attributeStart);
        } else if (attributeEnd - (attributeStart + 1) > 0) {
          nameToSearch = name.substring(attributeStart + 1, attributeEnd);
        }
      }
      String nameFinal = nameToSearch;
      files = myDumbService.runReadActionInSmartMode(() -> CacheManager.SERVICE.getInstance(element.getProject()).getFilesWithWord(nameFinal, UsageSearchContext.IN_CODE,
        (GlobalSearchScope) scope,
        false));

      if (element instanceof AsciiDocAttributeDeclarationName) {
        Collection<AsciiDocAttributeDeclaration> asciiDocAttributeDeclarations = AsciiDocAttributeDeclarationKeyIndex.getInstance().get(name, element.getProject(),
          // searching also the libraries is generally not useful; therefore restrict to project scope
          ((GlobalSearchScope) scope).intersectWith(GlobalSearchScope.projectScope(element.getProject()))
        );
        for (AsciiDocAttributeDeclaration attribute : asciiDocAttributeDeclarations) {
          AsciiDocAttributeDeclarationName child = attribute.getAttributeDeclarationName();
          if (child != null) {
            consumePsiElementForRename(consumer, child);
          }
        }
      }

      if (files.length == 0) {
        return;
      }
    } else if (scope instanceof LocalSearchScope) {
      localSearch = true;
      // when the IDE highlights references of the current file in the editor
      LocalSearchScope localSearchScope = (LocalSearchScope) scope;
      if (localSearchScope.getScope().length == 1 && localSearchScope.getScope()[0] instanceof PsiFile) {
        files = new PsiFile[]{(PsiFile) localSearchScope.getScope()[0]};
      } else {
        return;
      }
    } else {
      return;
    }

    // default, like for: AsciiDocAttributeDeclarationName
    boolean caseSensitive = false;
    if (element instanceof AsciiDocBlockId) {
      caseSensitive = true;
    }
    final StringSearcher searcher = new StringSearcher(name, caseSensitive, true, false);
    for (PsiFile psiFile : files) {
      if (psiFile.getLanguage() == AsciiDocLanguage.INSTANCE) {
        if (localSearch) {
          for (AsciiDocAttributeDeclaration attribute : PsiTreeUtil.findChildrenOfType(psiFile, AsciiDocAttributeDeclaration.class)) {
            if (name.toLowerCase(Locale.US).equals(attribute.getAttributeName().toLowerCase(Locale.US))) {
              AsciiDocAttributeDeclarationName child = attribute.getAttributeDeclarationName();
              if (child != null) {
                consumePsiElementForRename(consumer, child);
              }
            }
          }
        }
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

  private void consumePsiElementForRename(@NotNull Processor<? super PsiReference> consumer, PsiNamedElement element) {
    consumer.process(new PsiReferenceBase<PsiNamedElement>(element) {
      @Override
      public PsiElement resolve() {
        return getElement();
      }

      @NotNull
      @Override
      public TextRange getRangeInElement() {
        return new TextRange(0, getElement().getTextLength());
      }

      @Override
      public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        myElement.setName(newElementName);
        return myElement;
      }
    });
  }

  private void checkReference(@NotNull Processor<? super PsiReference> consumer, PsiElement element, PsiReference reference) {
    if (reference != null) {
      if (reference instanceof PsiPolyVariantReference) {
        for (ResolveResult resolved : ((PsiPolyVariantReference) reference).multiResolve(false)) {
          if (PsiManager.getInstance(element.getProject()).areElementsEquivalent(element, resolved.getElement())) {
            consumer.process(reference);
          }
        }
      } else {
        PsiElement resolved = reference.resolve();
        if (resolved != null) {
          if (PsiManager.getInstance(element.getProject()).areElementsEquivalent(element, resolved)) {
            consumer.process(reference);
          }
        }
      }
    }
  }

}

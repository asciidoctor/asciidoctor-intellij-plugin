package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
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
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationKeyIndex;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationName;
import org.asciidoc.intellij.psi.AsciiDocBlockId;
import org.asciidoc.intellij.psi.AsciiDocNamedElement;
import org.asciidoc.intellij.psi.AsciiDocSearchScope;
import org.asciidoc.intellij.psi.AsciiDocTagDeclaration;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.threading.AsciiDocProcessUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class AsciiDocIdReferencesSearch extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  protected AsciiDocIdReferencesSearch() {
    super(false);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters p, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement element = p.getElementToSearch();
    if (!(element instanceof AsciiDocNamedElement) && !(element instanceof PsiDirectory)) {
      return;
    }
    AsciiDocProcessUtil.runInReadActionWithWriteActionPriority(() -> {
      if (!element.isValid()) {
        return;
      }

      // use scope determined by user here, as effective search scope would return the module and its dependants
      SearchScope scope = p.getScopeDeterminedByUser();

      if (element instanceof AsciiDocNamedElement) {
        String name = ((AsciiDocNamedElement) element).getName();
        if (name != null && name.length() > 0) {
          search(consumer, element, name, scope);
        }
      } else //noinspection ConstantConditions
        if (element instanceof PsiDirectory) {
        String name = ((PsiDirectory) element).getName();
        if (name.endsWith("s") && name.length() > 1) {
          // it might be a partials, attachments, etc. directory; search for the family and attribute
          VirtualFile antoraModuleDir = AsciiDocUtil.findAntoraModuleDir(element);
          if (antoraModuleDir != null) {
            // partials -> partial$
            search(consumer, element, name.substring(0, name.length() - 1), scope);
            // partials -> partialsdir
            search(consumer, element, name + "dir", scope);
          }
        }
      }
    });
  }

  private void search(@NotNull Processor<? super PsiReference> consumer, PsiElement element, String name, SearchScope scope) {
    DumbService myDumbService = DumbService.getInstance(element.getProject());
    PsiFile[] files;
    boolean localSearch = false;
    if (scope instanceof GlobalSearchScope) {
      // always restrict to project files, never search libraries
      scope = ((GlobalSearchScope) scope).intersectWith(GlobalSearchScope.projectScope(element.getProject()));
      if (!(element instanceof AsciiDocTagDeclaration)) {
        scope = GlobalSearchScope.getScopeRestrictedByFileTypes((GlobalSearchScope) scope, AsciiDocFileType.INSTANCE);
      }
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

      GlobalSearchScope finalScope = (GlobalSearchScope) scope;
      files = myDumbService.runReadActionInSmartMode(() -> {
        short context = UsageSearchContext.IN_CODE;
        if (element instanceof AsciiDocTagDeclaration) {
          context = UsageSearchContext.ANY;
        }
        return CacheManager.getInstance(element.getProject())
          .getFilesWithWord(nameFinal, context, finalScope, false);
        }
      );

      if (element instanceof AsciiDocAttributeDeclarationName) {
        Collection<AsciiDocAttributeDeclaration> asciiDocAttributeDeclarations = AsciiDocAttributeDeclarationKeyIndex.getInstance().get(name, element.getProject(),
          // searching also the libraries is generally not useful; therefore restrict to project scope
          ((GlobalSearchScope) scope).intersectWith(new AsciiDocSearchScope(element.getProject()).restrictedByAsciiDocFileType())
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
    if (element instanceof AsciiDocBlockId || element instanceof AsciiDocTagDeclaration) {
      caseSensitive = true;
    }
    final StringSearcher asciidocSearcher = new StringSearcher(name, caseSensitive, true, false);
    final StringSearcher tagdeclarationSearcher = new StringSearcher("::" + name + "[]", caseSensitive, true, false);
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
      if (localSearch) {
        for (AsciiDocAttributeDeclaration attribute : PsiTreeUtil.findChildrenOfType(psiFile, AsciiDocAttributeDeclaration.class)) {
          if (attribute.matchesKey(name)) {
            AsciiDocAttributeDeclarationName child = attribute.getAttributeDeclarationName();
            if (child != null) {
              consumePsiElementForRename(consumer, child);
            }
          }
        }
      }
      final CharSequence text = ReadAction.compute(psiFile::getText);
      if (text == null) {
        LowLevelSearchUtil.processTexts(text, 0, text.length(), psiFile.getLanguage() == AsciiDocLanguage.INSTANCE ? asciidocSearcher : tagdeclarationSearcher, index -> {
          ProgressManager.checkCanceled();
          myDumbService.runReadActionInSmartMode(() -> {
            PsiReference referenceAt = psiFile.findReferenceAt(index + (psiFile.getLanguage() == AsciiDocLanguage.INSTANCE ? 0 : 2));
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

  }

  private void consumePsiElementForRename(@NotNull Processor<? super PsiReference> consumer, PsiNamedElement element) {
    consumer.process(new PsiReferenceBase<>(element) {
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

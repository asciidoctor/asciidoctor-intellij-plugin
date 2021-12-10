package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.util.SlowOperations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AsciiDocSimpleFileReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  private final TextRange myRangeInElement;

  public AsciiDocSimpleFileReference(@NotNull PsiElement element, TextRange textRange) {
    super(element, textRange);
    myRangeInElement = textRange;
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    List<ResolveResult> results = new ArrayList<>();
    String name = myRangeInElement.substring(myElement.getText());

    // Might be called from PsiViewerDialog in EDT thread.
    // This needs access to the file index to get the information we need.
    // remove SlowOperations from 2021.3 onwards, as several changes have been made to the caller to avoid this
    // https://github.com/JetBrains/intellij-community/commits/master/platform/lang-impl/src/com/intellij/internal/psiView/PsiViewerDialog.java
    SlowOperations.allowSlowOperations(() -> {
      PsiFile[] filesByName = FilenameIndex.getFilesByName(myElement.getProject(), name, new AsciiDocSearchScope(myElement.getProject()));
      for (PsiFile file : filesByName) {
        results.add(new PsiElementResolveResult(file));
      }
    });

    return results.toArray(new ResolveResult[0]);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
  }

}

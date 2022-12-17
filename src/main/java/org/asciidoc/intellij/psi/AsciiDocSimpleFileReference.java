package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.FilenameIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
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

    Collection<VirtualFile> filesByName = FilenameIndex.getVirtualFilesByName(name, new AsciiDocSearchScope(myElement.getProject()));
    for (VirtualFile file : filesByName) {
      PsiFile psiFile = PsiManager.getInstance(myElement.getProject()).findFile(file);
      if (psiFile != null) {
        results.add(new PsiElementResolveResult(psiFile));
      }
    }

    return results.toArray(new ResolveResult[0]);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) {
    return element;
  }

}

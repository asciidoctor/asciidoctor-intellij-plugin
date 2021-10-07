package org.asciidoc.intellij.psi;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.FilenameIndex;
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
    DumbService myDumbService = DumbService.getInstance(myElement.getProject());
    PsiFile[] filesByName = myDumbService.runReadActionInSmartMode(() -> FilenameIndex.getFilesByName(myElement.getProject(), name, new AsciiDocSearchScope(myElement.getProject())));
    for (PsiFile file : filesByName) {
      results.add(new PsiElementResolveResult(file));
    }
    return results.toArray(new ResolveResult[0]);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
  }

}

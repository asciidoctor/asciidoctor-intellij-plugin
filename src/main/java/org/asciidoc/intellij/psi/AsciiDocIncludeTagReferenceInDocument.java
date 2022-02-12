package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AsciiDocIncludeTagReferenceInDocument extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

  private final String key;

  public AsciiDocIncludeTagReferenceInDocument(@NotNull PsiElement element, TextRange textRange) {
    super(element, textRange);
    key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    List<ResolveResult> results = new ArrayList<>();
    PsiElement parent = myElement.getParent();
    if (parent != null) {
      PsiElement blockMacro = parent.getParent();
      if (blockMacro != null) {
        PsiReference[] references = blockMacro.getReferences();
        for (int i = references.length - 1; i >= 0; i--) {
          if (references[i] instanceof AsciiDocFileReference) {
            PsiElement resolve = references[i].resolve();
            if (resolve != null) {
              PsiTreeUtil.processElements(resolve.getContainingFile(), element -> {
                for (PsiReference reference : element.getReferences()) {
                  if (reference instanceof AsciiDocIncludeTagReferenceInElement) {
                    AsciiDocIncludeTagReferenceInElement tagReference = (AsciiDocIncludeTagReferenceInElement) reference;
                    if (tagReference.getType().equals("tag") && tagReference.getKey().equals(key)) {
                      results.add(new PsiElementResolveResult(new AsciiDocTagDeclaration(tagReference)));
                      return false;
                    }
                  }
                }
                return true;
              });
              return results.toArray(new ResolveResult[0]);
            }
            // only the last file reference is the one with the file
            // any preceding will be a directory that could contain many children with comments
            break;
          }
        }
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

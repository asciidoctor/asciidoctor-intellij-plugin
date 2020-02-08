package org.asciidoc.intellij.psi;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
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

public class AsciiDocIncludeTagReferenceInComment extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  private final String type;
  private String key;

  public AsciiDocIncludeTagReferenceInComment(@NotNull PsiElement element, TextRange textRange, String type) {
    super(element, textRange);
    this.type = type;
    key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    List<ResolveResult> results = new ArrayList<>();
    PsiTreeUtil.processElements(myElement.getContainingFile(), element -> {
      for (PsiReference reference : element.getReferences()) {
        if (reference instanceof AsciiDocIncludeTagReferenceInComment) {
          AsciiDocIncludeTagReferenceInComment tagReference = (AsciiDocIncludeTagReferenceInComment) reference;
          if (tagReference.getType().equals("tag") && tagReference.key.equals(key)) {
            // will result to the first tag with the given name in the file
            results.add(new PsiElementResolveResult(new AsciiDocTagDeclaration(tagReference)));
            break;
          }
        }
      }
      return true;
    });
    return results.toArray(new ResolveResult[0]);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    List<LookupElement> variants = new ArrayList<>();
    PsiTreeUtil.processElements(myElement.getContainingFile(), element -> {
      for (PsiReference reference : element.getReferences()) {
        if (reference instanceof AsciiDocIncludeTagReferenceInComment) {
          AsciiDocIncludeTagReferenceInComment tagReference = (AsciiDocIncludeTagReferenceInComment) reference;
          if (tagReference.getType().equals("tag")) {
            variants.add(LookupElementBuilder.create(tagReference.key));
          }
        }
      }
      return true;
    });
    return variants.toArray();
  }

  public String getType() {
    return type;
  }

  public String getKey() {
    return key;
  }
}

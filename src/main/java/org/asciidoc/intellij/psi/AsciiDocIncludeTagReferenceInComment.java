package org.asciidoc.intellij.psi;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
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
import java.util.Collection;
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
    Collection<PsiComment> psiComments = PsiTreeUtil.findChildrenOfType(myElement.getContainingFile(), PsiComment.class);
    for (PsiComment psiComment : psiComments) {
      PsiReference[] commentReferences = psiComment.getReferences();
      for (PsiReference commentReference : commentReferences) {
        if (commentReference instanceof AsciiDocIncludeTagReferenceInComment) {
          AsciiDocIncludeTagReferenceInComment reference = (AsciiDocIncludeTagReferenceInComment) commentReference;
          if (reference.getType().equals("tag") && reference.key.equals(key)) {
            results.add(new PsiElementResolveResult(new AsciiDocTagDeclaration(reference)));
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

  @NotNull
  @Override
  public Object[] getVariants() {
    List<LookupElement> variants = new ArrayList<>();
    Collection<PsiComment> psiComments = PsiTreeUtil.findChildrenOfType(myElement.getContainingFile(), PsiComment.class);
    for (PsiComment psiComment : psiComments) {
      PsiReference[] commentReferences = psiComment.getReferences();
      for (PsiReference commentReference : commentReferences) {
        if (commentReference instanceof AsciiDocIncludeTagReferenceInComment) {
          AsciiDocIncludeTagReferenceInComment reference = (AsciiDocIncludeTagReferenceInComment) commentReference;
          if (reference.getType().equals("tag")) {
            variants.add(LookupElementBuilder.create(reference.key));
          }
        }
      }
    }
    return variants.toArray();
  }

  public String getType() {
    return type;
  }

  public String getKey() {
    return key;
  }
}

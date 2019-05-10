package org.asciidoc.intellij.psi;

import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import icons.AsciiDocIcons;
import org.jetbrains.annotations.*;

import java.util.ArrayList;
import java.util.List;

public class AsciiDocReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  private String key;

  public AsciiDocReference(@NotNull PsiElement element, TextRange textRange) {
    super(element, textRange);
    key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    Project project = myElement.getProject();
    final List<AsciiDocBlockId> ids = AsciiDocUtil.findIds(project, key);
    List<ResolveResult> results = new ArrayList<>();
    for (AsciiDocBlockId id : ids) {
      results.add(new PsiElementResolveResult(id));
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
    Project project = myElement.getProject();
    List<AsciiDocBlockId> ids = AsciiDocUtil.findIds(project);
    List<LookupElement> variants = new ArrayList<>();
    for (final AsciiDocBlockId id : ids) {
      if (id.getId() != null && id.getId().length() > 0) {
        variants.add(LookupElementBuilder.create(id).
          withIcon(AsciiDocIcons.Asciidoc_Icon).
          withTypeText(id.getContainingFile().getName())
        );
      }
    }
    return variants.toArray();
  }
}

package org.asciidoc.intellij.psi;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import icons.AsciiDocIcons;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocAttributeDeclarationReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  private String key;

  public AsciiDocAttributeDeclarationReference(@NotNull PsiElement element, TextRange textRange) {
    super(element, textRange);
    key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    Project project = myElement.getProject();
    final List<AsciiDocAttributeDeclaration> declarations = AsciiDocUtil.findAttributes(project, key);
    List<ResolveResult> results = new ArrayList<>();
    for (AsciiDocAttributeDeclaration declaration : declarations) {
      results.add(new PsiElementResolveResult(declaration));
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
    List<AsciiDocAttributeDeclaration> declarations = AsciiDocUtil.findAttributes(project);
    List<LookupElement> variants = new ArrayList<>();
    for (final AsciiDocAttributeDeclaration declaration : declarations) {
      if (declaration.getText() != null) {
        String value = declaration.getAttributeValue();
        if (value == null) {
          value = "";
        } else {
          value = " (" + value + ")";
        }
        String attributeName = declaration.getAttributeName();
        variants.add(LookupElementBuilder.create(attributeName + "}").
          withIcon(AsciiDocIcons.ASCIIDOC_ICON).
          withPresentableText(attributeName + value).
          withTypeText(declaration.getContainingFile().getName())
        );
      }
    }
    return variants.toArray();
  }
}

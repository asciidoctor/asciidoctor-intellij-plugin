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
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
      results.add(new PsiElementResolveResult(declaration.getNavigationElement()));
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
        variants.add(LookupElementBuilder.create(attributeName)
          .withIcon(AsciiDocIcons.ASCIIDOC_ICON)
          .withTailText(value, true)
          .withTypeText(declaration.getContainingFile().getName())
          .withInsertHandler((insertionContext, item) -> {
            // the finalizing } hasn't been entered yet, autocomplete it here
            int offset = insertionContext.getStartOffset();
            PsiElement element = insertionContext.getFile().findElementAt(offset);
            if (element != null && element.getNode() != null
              && element.getNode().getElementType() != AsciiDocTokenTypes.ATTRIBUTE_REF) {
              offset += attributeName.length();
              insertionContext.getDocument().insertString(offset, "}");
              offset += 1;
              insertionContext.getEditor().getCaretModel().moveToOffset(offset);
            }
          })
        );
      }
    }
    return variants.toArray();
  }
}

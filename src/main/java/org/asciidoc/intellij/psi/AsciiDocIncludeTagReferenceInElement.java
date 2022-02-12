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
import org.asciidoc.intellij.findUsages.AsciiDocReferenceContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Reference ony any element for an AsciiDoc include tag.
 * Can have the form of line comments (like in Java) or tags (like in XML).
 * <br>
 * Usually something like this would appear in a comment only. On the other hand it can also appear in standard
 * text files where IntelliJ doesn't recognize any comments. Therefore keep it "as is" and look in all elements
 * for tags.
 * <pre>
 * // tag::snippet-2[]
 * something
 * // end::snippet-2[]
 * </pre>
 *
 * <pre>
 * &lt;!-- tag::snippet-2[] -->
 * something
 * &lt;!-- end::snippet-2[] -->
 * </pre>
 *
 */
public class AsciiDocIncludeTagReferenceInElement extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

  private final String type;
  private final String key;

  public AsciiDocIncludeTagReferenceInElement(@NotNull PsiElement element, TextRange textRange, String type) {
    super(element, textRange);
    this.type = type;
    key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    List<ResolveResult> results = new ArrayList<>();
    PsiTreeUtil.processElements(myElement.getContainingFile(), element -> {
      Matcher matcher = AsciiDocReferenceContributor.TAG_PATTERN.matcher(element.getText());
      // using a matcher on the element's text first avoids generating unnecessary references on other elements
      if (matcher.find()) {
        for (PsiReference reference : element.getReferences()) {
          if (reference instanceof AsciiDocIncludeTagReferenceInElement) {
            AsciiDocIncludeTagReferenceInElement tagReference = (AsciiDocIncludeTagReferenceInElement) reference;
            if (tagReference.getType().equals("tag") && tagReference.key.equals(key)) {
              // will result to the first tag with the given name in the file
              results.add(new PsiElementResolveResult(new AsciiDocTagDeclaration(tagReference)));
              break;
            }
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

  @Override
  public Object @NotNull [] getVariants() {
    List<LookupElement> variants = new ArrayList<>();
    PsiTreeUtil.processElements(myElement.getContainingFile(), element -> {
      for (PsiReference reference : element.getReferences()) {
        if (reference instanceof AsciiDocIncludeTagReferenceInElement) {
          AsciiDocIncludeTagReferenceInElement tagReference = (AsciiDocIncludeTagReferenceInElement) reference;
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

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) {
    return element;
  }

}

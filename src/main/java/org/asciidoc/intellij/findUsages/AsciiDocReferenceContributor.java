package org.asciidoc.intellij.findUsages;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PlatformPatterns.psiFile;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocRef;
import org.asciidoc.intellij.psi.AsciiDocReference;
import org.jetbrains.annotations.NotNull;

public class AsciiDocReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

    final PsiElementPattern.Capture<AsciiDocRef> refCapture =
      psiElement(AsciiDocRef.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(refCapture,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext
                                                       context) {
          int start = 0;
          PsiElement child = element.getFirstChild();
          while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.REF) {
            start += child.getTextLength();
            child = child.getNextSibling();
          }
          if (child != null) {
            return new PsiReference[]{
              new AsciiDocReference(element, TextRange.create(start, start + child.getTextLength()))
            };
          } else {
            return new PsiReference[]{};
          }
        }
      });

    final PsiElementPattern.Capture<AsciiDocLink> linkCapture =
      psiElement(AsciiDocLink.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(linkCapture,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
          @NotNull ProcessingContext
            context) {
          List<PsiReference> references = new ArrayList<>();

          PsiReference anchorReference = findAnchor(element);
          if(anchorReference!=null) {
            references.add(anchorReference);
          }

          List<PsiReference> fileReferences = findFileReferences(element);

          references.addAll(fileReferences);

          return references.toArray(new PsiReference[0]);
        }
      });
  }

  private PsiReference findAnchor(PsiElement element) {
    int start = 0;
    PsiElement child = element.getFirstChild();
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.LINKANCHOR) {
      start += child.getTextLength();
      child = child.getNextSibling();
    }
    return child!=null ? new AsciiDocReference(element, TextRange.create(start, start + child.getTextLength())) : null;
  }

  private List<PsiReference> findFileReferences(PsiElement element) {
    int start = 0;
    PsiElement child = element.getFirstChild();
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.LINKFILE) {
      start += child.getTextLength();
      child = child.getNextSibling();
    }
    return Arrays.asList(new FileReferenceSet(child.getText(), element, start,null, false).getAllReferences());
  }
}

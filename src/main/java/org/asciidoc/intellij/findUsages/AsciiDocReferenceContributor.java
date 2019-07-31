package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.paths.WebReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationReference;
import org.asciidoc.intellij.psi.AsciiDocAttributeReference;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocRef;
import org.asciidoc.intellij.psi.AsciiDocReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PlatformPatterns.psiFile;

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

    final PsiElementPattern.Capture<AsciiDocAttributeReference> attributeReferenceCapture =
      psiElement(AsciiDocAttributeReference.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(attributeReferenceCapture, new PsiReferenceProvider() {
      @NotNull
      @Override
      public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                   @NotNull ProcessingContext
                                                     context) {
        int start = 0;
        PsiElement child = element.getFirstChild();
        while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ATTRIBUTE_REF) {
          start += child.getTextLength();
          child = child.getNextSibling();
        }
        if (child != null) {
          return new PsiReference[]{
            new AsciiDocAttributeDeclarationReference(element, TextRange.create(start, start + child.getTextLength()))
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
          if (anchorReference != null) {
            references.add(anchorReference);
          }

          List<PsiReference> fileReferences = findFileReferences(element);
          references.addAll(fileReferences);

          List<PsiReference> urlReferences = findUrlReferences(element);
          references.addAll(urlReferences);

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
    return child != null ? new AsciiDocReference(element, TextRange.create(start, start + child.getTextLength())) : null;
  }

  static class LinkFileReferenceSet extends FileReferenceSet {
    LinkFileReferenceSet(String str, @NotNull PsiElement element, int startInElement, @Nullable PsiReferenceProvider provider, boolean isCaseSensitive) {
      super(str, element, startInElement, provider, isCaseSensitive);
    }

    @Override
    protected void reparse() {
      super.reparse();
      if (myReferences.length > 0) {
        FileReference fileReference = myReferences[myReferences.length - 1];
        // if the reference can't find a file, try with an added '.adoc' extension
        if (fileReference.resolve() == null) {
          FileReference withExtension = createFileReference(fileReference.getRangeInElement(), fileReference.getIndex(), fileReference.getText() + ".adoc");
          if (withExtension.resolve() != null) {
            myReferences[myReferences.length - 1] = withExtension;
          }
        }
      }
    }
  }

  private List<PsiReference> findFileReferences(PsiElement element) {
    PsiElement child = element.getFirstChild();
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.LINKFILE) {
      child = child.getNextSibling();
    }
    if (child != null) {
      return Arrays.asList(new LinkFileReferenceSet(child.getText(), element, child.getStartOffsetInParent(), null, false).getAllReferences());
    } else {
      return Collections.emptyList();
    }
  }

  private List<PsiReference> findUrlReferences(PsiElement element) {
    PsiElement child = element.getFirstChild();
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.URL_LINK) {
      child = child.getNextSibling();
    }
    if (child != null) {
      return Collections.singletonList(new WebReference(
        element,
        TextRange.create(child.getStartOffsetInParent(), child.getStartOffsetInParent() + child.getTextLength()))
      );
    } else {
      return Collections.emptyList();
    }
  }

}

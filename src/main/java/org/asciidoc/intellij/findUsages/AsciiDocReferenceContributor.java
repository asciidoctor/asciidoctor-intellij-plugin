package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.paths.WebReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang3.StringEscapeUtils;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationReference;
import org.asciidoc.intellij.psi.AsciiDocAttributeReference;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocRef;
import org.asciidoc.intellij.psi.AsciiDocReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

          List<PsiReference> urlReferences = findUrlReferencesInLinks(element);
          references.addAll(urlReferences);

          return references.toArray(new PsiReference[0]);
        }
      });

    final PsiElementPattern.Capture<AsciiDocAttributeDeclaration> attributeDeclaration =
      psiElement(AsciiDocAttributeDeclaration.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(attributeDeclaration,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext context) {
          List<PsiReference> references = findUrlReferencesInAttributeDefinition(element);
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

  private List<PsiReference> findFileReferences(PsiElement element) {
    PsiElement child = element.getFirstChild();
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.LINKFILE) {
      child = child.getNextSibling();
    }
    if (child != null) {
      String file = child.getText();
      ArrayList<PsiReference> references = new ArrayList<>();
      int start = 0;
      for (int i = 0; i < file.length(); ++i) {
        if (file.charAt(i) == '/') {
          references.add(
            new AsciiDocFileReference(element, "link", file.substring(0, start),
              TextRange.create(child.getStartOffsetInParent() + start, child.getStartOffsetInParent() + i)
            )
          );
          start = i + 1;
        }
      }
      references.add(
        new AsciiDocFileReference(element, "link", file.substring(0, start),
          TextRange.create(child.getStartOffsetInParent() + start, child.getStartOffsetInParent() + file.length())
        )
      );
      return references;
    } else {
      return Collections.emptyList();
    }
  }

  private List<PsiReference> findUrlReferencesInLinks(PsiElement element) {
    PsiElement child = element.getFirstChild();
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.URL_LINK) {
      child = child.getNextSibling();
    }
    if (child != null) {
      String url = child.getText();
      url = url.replaceAll("\\+\\+\\+(.*)\\+\\+\\+", "$1");
      url = url.replaceAll("\\+\\+(.*)\\+\\+", "$1");
      return Collections.singletonList(new WebReference(
        element,
        TextRange.create(child.getStartOffsetInParent(), child.getStartOffsetInParent() + child.getTextLength()),
        StringEscapeUtils.unescapeHtml4(url))
      );
    } else {
      return Collections.emptyList();
    }
  }

  private Pattern urlInAttributeVal = Pattern.compile("(https?|file|ftp|irc)://[^\\s\\[\\]<]*([^\\s\\[\\]])");

  private List<PsiReference> findUrlReferencesInAttributeDefinition(PsiElement element) {
    PsiElement child = element.getFirstChild();
    List<PsiReference> result = new ArrayList<>();
    while (child != null && child.getNode().getElementType() != AsciiDocTokenTypes.ATTRIBUTE_VAL) {
      child = child.getNextSibling();
    }
    if (child != null) {
      Matcher urls = urlInAttributeVal.matcher(child.getText());
      while (urls.find()) {
        result.add(new WebReference(
          element,
          TextRange.create(child.getStartOffsetInParent() + urls.start(),
            child.getStartOffsetInParent() + urls.end()),
          StringEscapeUtils.unescapeHtml4(urls.group())
        ));
      }
    }
    return result;
  }

}

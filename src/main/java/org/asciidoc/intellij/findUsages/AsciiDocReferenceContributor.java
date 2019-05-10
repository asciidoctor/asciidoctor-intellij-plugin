package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocRef;
import org.asciidoc.intellij.psi.AsciiDocReference;
import org.jetbrains.annotations.NotNull;

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
  }
}

package org.asciidoc.intellij.findUsages;

import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocJavaReference;
import org.asciidoc.intellij.psi.AsciiDocTextItalic;
import org.asciidoc.intellij.psi.AsciiDocTextMono;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PlatformPatterns.psiFile;

public class AsciiDocJavaReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

    final PsiElementPattern.Capture<AsciiDocTextMono> monoCapture =
      psiElement(AsciiDocTextMono.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(monoCapture,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext context) {
          List<PsiReference> references = findReferencesElement(element);
          return references.toArray(new PsiReference[0]);
        }
      });

    final PsiElementPattern.Capture<AsciiDocTextItalic> italicCapture =
      psiElement(AsciiDocTextItalic.class).inFile(psiFile(AsciiDocFile.class));

    registrar.registerReferenceProvider(italicCapture,
      new PsiReferenceProvider() {
        @NotNull
        @Override
        public PsiReference[] getReferencesByElement(@NotNull PsiElement element,
                                                     @NotNull ProcessingContext context) {
          List<PsiReference> references = findReferencesElement(element);
          return references.toArray(new PsiReference[0]);
        }
      });
  }

  private List<PsiReference> findReferencesElement(PsiElement element) {
    ArrayList<PsiReference> references = new ArrayList<>();
    references.add(new AsciiDocJavaReference(element, TextRange.create(0, element.getTextLength())));
    return references;
  }

}

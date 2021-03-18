package org.asciidoc.intellij.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.highlighting.AsciiDocSyntaxHighlighter;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.psi.AsciiDocTextItalic;
import org.asciidoc.intellij.psi.AsciiDocTextMono;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Analyze the PSI add additional highlighting.
 * Currently adds italic font type and monospaced background to nested elements.
 *
 * @author Alexander Schwartz 2021
 */
public class AsciiDocExternalAnnotatorHighlighting extends com.intellij.lang.annotation.ExternalAnnotator<
  String, String> {
  @Override
  public @Nullable String collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
    // dummy implementation, just return anything
    return "";
  }

  @Override
  public @Nullable String doAnnotate(String collectedInfo) {
    // dummy implementation, just return anything
    return "";
  }

  @Override
  public void apply(@NotNull PsiFile file, String annotationResult, @NotNull AnnotationHolder holder) {
    AsciiDocVisitor visitor = new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        super.visitElement(element);
        if (element instanceof AsciiDocTextMono) {
          holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .enforcedTextAttributes(AsciiDocSyntaxHighlighter.ASCIIDOC_MONO.getDefaultAttributes())
            .range(element)
            .create();
        } else if (element instanceof AsciiDocTextItalic) {
          holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .enforcedTextAttributes(AsciiDocSyntaxHighlighter.ASCIIDOC_ITALIC.getDefaultAttributes())
            .range(element)
            .create();
        }
        PsiElement child = element.getFirstChild();
        while (child != null) {
          visitElement(child);
          child = child.getNextSibling();
        }
      }
    };
    visitor.visitFile(file);
  }

}

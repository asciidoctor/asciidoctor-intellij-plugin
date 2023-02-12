package org.asciidoc.intellij.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.highlighting.AsciiDocSyntaxHighlighter;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.psi.AsciiDocTextQuoted;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Analyze the PSI add additional highlighting.
 * Currently adds italic font type and monospaced background to nested elements.
 *
 * @author Alexander Schwartz 2021
 */
public class AsciiDocExternalAnnotatorHighlighting extends ExternalAnnotator<String, String> implements DumbAware {
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
        if (element instanceof AsciiDocTextQuoted) {
          boolean isMono = ((AsciiDocTextQuoted) element).isMono();
          boolean isItalic = ((AsciiDocTextQuoted) element).isItalic();
          boolean isBold = ((AsciiDocTextQuoted) element).isBold();
          if (isMono && isBold && isItalic) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(AsciiDocSyntaxHighlighter.ASCIIDOC_MONOBOLDITALIC.getDefaultAttributes())
              .range(element)
              .create();
          } else if (isMono && isItalic) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(AsciiDocSyntaxHighlighter.ASCIIDOC_MONOITALIC.getDefaultAttributes())
              .range(element)
              .create();
          } else if (isBold && isItalic) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(AsciiDocSyntaxHighlighter.ASCIIDOC_BOLDITALIC.getDefaultAttributes())
              .range(element)
              .create();
          } else if (isMono) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(AsciiDocSyntaxHighlighter.ASCIIDOC_MONO.getDefaultAttributes())
              .range(element)
              .create();
          } else if (isItalic) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(AsciiDocSyntaxHighlighter.ASCIIDOC_ITALIC.getDefaultAttributes())
              .range(element)
              .create();
          } else if (isBold) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(AsciiDocSyntaxHighlighter.ASCIIDOC_BOLD.getDefaultAttributes())
              .range(element)
              .create();
          }
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

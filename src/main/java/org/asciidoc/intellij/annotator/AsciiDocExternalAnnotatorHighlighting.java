package org.asciidoc.intellij.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
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
              .enforcedTextAttributes(attributesOf(AsciiDocSyntaxHighlighter.ASCIIDOC_MONOBOLDITALIC))
              .range(element)
              .create();
          } else if (isMono && isItalic) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(attributesOf(AsciiDocSyntaxHighlighter.ASCIIDOC_MONOITALIC))
              .range(element)
              .create();
          } else if (isBold && isItalic) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(attributesOf(AsciiDocSyntaxHighlighter.ASCIIDOC_BOLDITALIC))
              .range(element)
              .create();
          } else if (isMono) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(attributesOf(AsciiDocSyntaxHighlighter.ASCIIDOC_MONO))
              .range(element)
              .create();
          } else if (isItalic) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(attributesOf(AsciiDocSyntaxHighlighter.ASCIIDOC_ITALIC))
              .range(element)
              .create();
          } else if (isBold) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
              .enforcedTextAttributes(attributesOf(AsciiDocSyntaxHighlighter.ASCIIDOC_BOLD))
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

  private static TextAttributes attributesOf(TextAttributesKey key) {
    // Ensure that we get the theme of the editor, and not the theme of the IDE to determine the attributes.
    // Otherwise we'll apply the wrong background color to monospaced contents.
    String schema = EditorColorsManager.getInstance().isDarkEditor() ? "Darcula" : EditorColorsScheme.DEFAULT_SCHEME_NAME;
    return EditorColorsManager.getInstance().getScheme(schema).getAttributes(key);
  }

}

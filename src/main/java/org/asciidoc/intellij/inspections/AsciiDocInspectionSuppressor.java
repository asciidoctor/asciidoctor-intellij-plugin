package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AsciiDocInspectionSuppressor implements InspectionSuppressor {
  private static final Logger LOG = Logger.getInstance(AsciiDocInspectionSuppressor.class);

  @Override
  @NotNull
  public SuppressQuickFix[] getSuppressActions(final PsiElement element, @NotNull final String toolId) {
    return new SuppressQuickFix[]{new SuppressSingleLine(toolId), new SuppressForFile(toolId)};
  }

  @Override
  public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
    PsiElement prev = startOfLine(element);
    while (prev instanceof PsiWhiteSpace || prev instanceof PsiComment) {
      if (prev instanceof PsiComment) {
        @NonNls String text = prev.getText();
        if (text.contains("suppress") && text.contains("\"" + toolId + "\"")) {
          return true;
        }
      }
      prev = prev.getPrevSibling();
    }

    PsiElement leaf = element.getContainingFile().findElementAt(0);

    while (leaf instanceof PsiWhiteSpace || leaf instanceof PsiComment) {
      if (leaf instanceof PsiComment) {
        @NonNls String text = leaf.getText();
        if (text.contains("suppress") && text.contains("\"" + toolId + "\"") && text.contains("file")) {
          return true;
        }
      }
      leaf = leaf.getNextSibling();
    }

    return false;
  }

  private static PsiElement startOfLine(@NotNull PsiElement element) {
    if (element instanceof PsiFile) {
      return null;
    }
    PsiElement prev = element.getPrevSibling();
    while (prev != null) {
      if (prev instanceof PsiFile) {
        return null;
      }
      if (prev instanceof PsiWhiteSpace && prev.getText().equals("\n")) {
        break;
      }
      if (prev.getPrevSibling() == null && !(prev.getParent() instanceof PsiFile)) {
        prev = prev.getParent();
      } else {
        prev = prev.getPrevSibling();
      }
    }
    return prev;
  }

  private static class SuppressSingleLine implements SuppressQuickFix {
    private final String shortName;

    private SuppressSingleLine(String shortName) {
      this.shortName = shortName;
    }

    @Override
    @NotNull
    public String getFamilyName() {
      return "Suppress for this line";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      final PsiElement element = descriptor.getStartElement();
      final PsiFile file = element.getContainingFile();
      PsiElement prev = startOfLine(element);

      int start = 0;
      if (prev != null) {
        start = prev.getTextOffset() + prev.getTextLength();
      }

      @NonNls final Document doc = PsiDocumentManager.getInstance(project).getDocument(file);
      LOG.assertTrue(doc != null);
      final int line = doc.getLineNumber(start);
      final int lineStart = doc.getLineStartOffset(line);

      doc.insertString(lineStart, "// suppress inspection \"" + shortName +
        "\"\n");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull PsiElement context) {
      return context.getLanguage() == AsciiDocLanguage.INSTANCE;
    }

    @Override
    public boolean isSuppressAll() {
      return false;
    }
  }

  private static class SuppressForFile implements SuppressQuickFix {
    private final String shortName;

    private SuppressForFile(String shortName) {
      this.shortName = shortName;
    }

    @Override
    @NotNull
    public String getFamilyName() {
      return "Suppress for this file";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      final PsiElement element = descriptor.getStartElement();
      final PsiFile file = element.getContainingFile();

      @NonNls final Document doc = PsiDocumentManager.getInstance(project).getDocument(file);
      LOG.assertTrue(doc != null, file);

      doc.insertString(0, "// suppress inspection \"" +
        shortName +
        "\" for whole file\n");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull PsiElement context) {
      return context.isValid() && context.getContainingFile() instanceof AsciiDocFile;
    }

    @Override
    public boolean isSuppressAll() {
      return false;
    }

  }

}

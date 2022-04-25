package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.inspections.AsciiDocPassthroughInspection;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocTextQuoted;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fatih Bozik
 */
public class AsciiDocConvertPassthrough implements LocalQuickFix {
  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.convertPassthrough");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    final PsiElement element = descriptor.getPsiElement();
    if (AsciiDocPassthroughInspection.isObsoletePassthroughStart(element) ||
      AsciiDocPassthroughInspection.isObsoletePassthroughEnd(element)) {
      ((LeafElement) element).replaceWithText("++");
    } else if (AsciiDocPassthroughInspection.isQuotedTextWithObsoletePassthrough(element)) {
      String text = convertPassthrough((AsciiDocTextQuoted) element);
      if (text != null) {
        element.replace(createPassthrough(project, text));
      }
    }
  }

  private String convertPassthrough(AsciiDocTextQuoted element) {
    StringBuilder result = new StringBuilder();
    result.append(AsciiDocTextQuoted.getBodyRange(element).shiftLeft(element.getTextOffset()).substring(element.getText()));
    String quote = "++";
    if (result.length() > 0 && (result.charAt(0) == quote.charAt(0) || result.charAt(result.length() - 1) == quote.charAt(0))) {
      if (result.indexOf("[") == -1 && result.indexOf("]") == -1) {
        result.insert(0, "pass:[");
        result.append("]");
        quote = "";
      } else {
        return null;
      }
    } else if (result.indexOf(quote) != -1) {
      quote = "+++";
      if (result.indexOf(quote) != -1) {
        if (result.indexOf("[") == -1 && result.indexOf("]") == -1) {
          result.insert(0, "pass:[");
          result.append("]");
          quote = "";
        } else {
          return null;
        }
      }
    }
    result.insert(0, quote);
    result.append(quote);
    return result.toString();
  }


  @NotNull
  private static PsiElement createPassthrough(@NotNull Project project, @NotNull String text) {
    AsciiDocFile file = AsciiDocUtil.createFileFromText(project, text);
    PsiElement child = file.getFirstChild();
    if (child.getNextSibling() != null) {
      throw new IllegalStateException("created two children where there should only be one: " + text);
    }
    child = child.getFirstChild();
    if (child.getNextSibling() != null) {
      throw new IllegalStateException("created two children where there should only be one: " + text);
    }
    if (!child.getText().equals(text)) {
      throw new IllegalStateException("text differs: " + child.getText() + " vs. " + text);
    }
    return child;
  }

}

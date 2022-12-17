package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocHeading;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author Alexander Schwartz 2016
 */
public class AsciiDocConvertMarkdownHeading implements LocalQuickFix {

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.convertMarkdownHeading");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    StringBuilder text = new StringBuilder(element.getText());
    for (int i = 0; i < text.length(); ++i) {
      if (text.charAt(i) == '#') {
        text.setCharAt(i, '=');
      } else {
        break;
      }
    }
    element.replace(createHeading(project, text.toString()));
  }

  @NotNull
  private static PsiElement createHeading(@NotNull Project project, @NotNull String text) {
    AsciiDocFile file = AsciiDocUtil.createFileFromText(project, text);
    AsciiDocSection section = PsiTreeUtil.findChildOfType(file, AsciiDocSection.class);
    Objects.requireNonNull(section, "text passed as parameter should have led to a section");
    AsciiDocHeading heading = PsiTreeUtil.findChildOfType(section, AsciiDocHeading.class);
    Objects.requireNonNull(heading, "there should be a heading from the text passed as argument");
    return heading;
  }

}

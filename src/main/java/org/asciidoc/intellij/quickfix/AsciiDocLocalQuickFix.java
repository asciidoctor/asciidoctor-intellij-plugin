package org.asciidoc.intellij.quickfix;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public abstract class AsciiDocLocalQuickFix implements LocalQuickFix {

  @Override
  public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
    try {
      return LocalQuickFix.super.generatePreview(project, previewDescriptor);
    } catch (IncorrectOperationException ex) {
      return IntentionPreviewInfo.EMPTY;
    }
  }
}

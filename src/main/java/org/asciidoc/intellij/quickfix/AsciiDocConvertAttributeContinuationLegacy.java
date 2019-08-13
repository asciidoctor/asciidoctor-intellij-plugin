package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFixBase;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AsciiDocConvertAttributeContinuationLegacy extends LocalQuickFixBase {
  public static final String NAME = "Convert to modern AsciiDoc Attribute continuation";

  public AsciiDocConvertAttributeContinuationLegacy() {
    super(NAME);
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    element.replace(createContinuation(project, element.getText()));
  }

  @NotNull
  private static PsiElement createContinuation(@NotNull Project project, String oldPattern) {
    oldPattern = oldPattern.replace('+', '\\');
    AsciiDocFile file = AsciiDocUtil.createFileFromText(project, ":attr: val " + oldPattern + "val");
    ASTNode continuation = file.getFirstChild().getNode().findChildByType(AsciiDocTokenTypes.ATTRIBUTE_CONTINUATION);
    Objects.requireNonNull(continuation);
    return continuation.getPsi();
  }

}

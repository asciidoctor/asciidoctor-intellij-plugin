package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationImpl;
import org.jetbrains.annotations.NotNull;

/**
 * Check if the length of the description is within the recommended size for Google search.
 *
 * @author Alexander Schwartz 2020
 */
public class AsciiDocDescriptionLengthInspection extends AsciiDocInspectionBase {
  private static final int MAX_LENGTH = 155;
  private static final String TEXT_HINT_DESCRIPTION_TOO_LONG = "Description longer than " + MAX_LENGTH + " characters: shorten by ";

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
        if (o instanceof AsciiDocAttributeDeclarationImpl) {
          AsciiDocAttributeDeclarationImpl declaration = (AsciiDocAttributeDeclarationImpl) o;
          if ("description".equalsIgnoreCase(declaration.getAttributeName())) {
            String value = declaration.getAttributeValue();
            if (value != null && value.length() > MAX_LENGTH) {
              holder.registerProblem(o, TEXT_HINT_DESCRIPTION_TOO_LONG + (value.length() - MAX_LENGTH), ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            }
          }
        }
        super.visitElement(o);
      }
    };
  }
}

package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationReference;
import org.asciidoc.intellij.psi.AsciiDocAttributeReference;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.psi.AttributeDeclaration;
import org.asciidoc.intellij.quickfix.AsciiDocChangeToPassthrough;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * @author Alexander Schwartz 2021
 */
public class AsciiDocAttributeUndefinedInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_ATTRIBUTE_NOT_DEFINED = "Attribute should be defined";

  private static final AsciiDocChangeToPassthrough CHANGE_TO_PASSTHROUGH = new AsciiDocChangeToPassthrough();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
        if (o instanceof AsciiDocAttributeReference) {
          if (isExcludedByIfdef(o, true)) {
            return;
          }
          @NotNull PsiReference[] references = ((AsciiDocAttributeReference) o).getReferences();
          for (PsiReference reference : references) {
            if (!(reference instanceof AsciiDocAttributeDeclarationReference attributeDeclarationReference)) {
              continue;
            }
            String key = attributeDeclarationReference.getValue().toLowerCase(Locale.US);
            // check for built-in attributes
            if (AsciiDocBundle.getBuiltInAttributesList().contains(key)) {
              continue;
            }
            // each environment has it own suffix like env-github, treat them as defined
            // https://docs.asciidoctor.org/asciidoc/latest/attributes/document-attributes-ref/
            if (key.startsWith("env-")) {
              continue;
            }
            if (key.startsWith("basebackend-")) {
              continue;
            }
            if (key.startsWith("backend-")) {
              continue;
            }
            if (key.startsWith("filetype-")) {
              continue;
            }
            if (key.startsWith("doctype-")) {
              continue;
            }
            // check for attributes defined in settings or in Antora component descriptor
            List<AttributeDeclaration> attributes = AsciiDocUtil.findAttributes(o.getProject(), key, o);
            if (!attributes.isEmpty()) {
              continue;
            }
            if (attributeDeclarationReference.multiResolve(false).length == 0) {
              LocalQuickFix[] fixes = new LocalQuickFix[]{CHANGE_TO_PASSTHROUGH};
              holder.registerProblem(o, TEXT_HINT_ATTRIBUTE_NOT_DEFINED, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, reference.getRangeInElement(), fixes);
            }
          }
        }
        super.visitElement(o);
      }
    };
  }
}

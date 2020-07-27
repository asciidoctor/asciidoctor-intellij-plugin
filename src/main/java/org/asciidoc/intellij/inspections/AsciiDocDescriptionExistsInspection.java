package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocSectionImpl;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.quickfix.AsciiDocAddDescriptionPageAttribute;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Check if a description exists for an Antora page.
 *
 * @author Alexander Schwartz 2020
 */
public class AsciiDocDescriptionExistsInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_DESCRIPTION_MISSING = "Description attribute is missing";
  private static final AsciiDocAddDescriptionPageAttribute DESCRIPTION_MISSING_QUICKFIX = new AsciiDocAddDescriptionPageAttribute();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
        if (o instanceof AsciiDocSectionImpl) {
          AsciiDocSectionImpl section = (AsciiDocSectionImpl) o;
          if (section.getHeadingLevel() == 1) {
            VirtualFile antoraPagesDir = AsciiDocUtil.findAntoraPagesDir(o);
            if (antoraPagesDir != null) {
              String myPath = o.getContainingFile().getVirtualFile().getCanonicalPath();
              String pagesPath = antoraPagesDir.getCanonicalPath();
              if (myPath != null && pagesPath != null && myPath.startsWith(antoraPagesDir.getCanonicalPath())) {
                Collection<AsciiDocAttributeDeclaration> pageAttributes = AsciiDocUtil.findPageAttributes(o.getContainingFile());
                boolean found = false;
                for (AsciiDocAttributeDeclaration pageAttribute : pageAttributes) {
                  if ("description".equalsIgnoreCase(pageAttribute.getAttributeName())) {
                    found = true;
                    break;
                  }
                }
                if (!found) {
                  holder.registerProblem(section.getHeadingElement(), TEXT_HINT_DESCRIPTION_MISSING, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, DESCRIPTION_MISSING_QUICKFIX);
                }
              }
            }
          }
        }
        super.visitElement(o);
      }
    };
  }
}

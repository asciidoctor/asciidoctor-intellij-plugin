package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.psi.HasAntoraReference;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;

/**
 * @author Alexander Schwartz 2022
 */
public class AsciiDocAntoraModuleResolveInspection extends AsciiDocInspectionBase {
  private static final @InspectionMessage String TEXT_HINT_MODULE_DOESNT_RESOLVE = "Antora Module doesn't resolve";

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
        if (o instanceof HasAntoraReference) {
          AsciiDocFileReference file = ((HasAntoraReference) o).getAntoraReference();
          if (file != null) {
            String key = file.getRangeInElement().substring(o.getText());
            Matcher version = AsciiDocUtil.VERSION.matcher(key);
            if (version.find()) {
              if (!AsciiDocUtil.antoraVersionAndComponentExist(o, key + ":")) {
                return;
              }
            }
            ResolveResult @NotNull [] resolved = file.multiResolve(false);
            if (resolved.length == 0) {
              holder.registerProblem(o, TEXT_HINT_MODULE_DOESNT_RESOLVE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                file.getRangeInElement());
            }
          }
        }
        super.visitElement(o);
      }
    };
  }

}

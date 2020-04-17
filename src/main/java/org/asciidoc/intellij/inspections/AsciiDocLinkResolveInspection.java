package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import org.asciidoc.intellij.psi.AsciiDocAttributeInBrackets;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocInlineMacro;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.psi.HasAnchorReference;
import org.asciidoc.intellij.psi.HasAntoraReference;
import org.asciidoc.intellij.psi.HasFileReference;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFile;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFileQuickfix;
import org.jetbrains.annotations.NotNull;

import java.util.StringTokenizer;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocLinkResolveInspection extends AsciiDocInspectionBase {
  private static final String TEXT_HINT_FILE_DOESNT_RESOLVE = "File doesn't resolve";
  private static final String TEXT_HINT_ANCHOR_DOESNT_RESOLVE = "Anchor doesn't resolve";
  private static final AsciiDocChangeCaseForAnchor CHANGE_CASE_FOR_ANCHOR = new AsciiDocChangeCaseForAnchor();
  private static final AsciiDocCreateMissingFileQuickfix CREATE_FILE = new AsciiDocCreateMissingFileQuickfix();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(PsiElement o) {
        boolean continueResolving = true;
        if (o instanceof AsciiDocBlockMacro) {
          AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) o;
          if (blockMacro.getMacroName().equals("include")) {
            AsciiDocAttributeInBrackets opts = blockMacro.getAttribute("opts");
            if (opts != null) {
              String value = opts.getAttrValue();
              if (value != null) {
                StringTokenizer st = new StringTokenizer(value, ",");
                while (st.hasMoreElements()) {
                  if (st.nextToken().trim().equals("optional")) {
                    continueResolving = false;
                  }
                }
              }
            }
          }
        }
        if (continueResolving &&
          (o instanceof AsciiDocLink || o instanceof AsciiDocBlockMacro || o instanceof AsciiDocInlineMacro)) {
          String resolvedBody;
          if (o instanceof AsciiDocLink) {
            resolvedBody = ((AsciiDocLink) o).getResolvedBody();
          } else if (o instanceof AsciiDocBlockMacro) {
            resolvedBody = ((AsciiDocBlockMacro) o).getResolvedBody();
          } else {
            resolvedBody = ((AsciiDocInlineMacro) o).getResolvedBody();
          }
          if (resolvedBody == null) {
            return;
          } else if (AsciiDocUtil.URL_PREFIX_PATTERN.matcher(resolvedBody).find()) {
            // this is a URL, don't
            return;
          } else if (resolvedBody.startsWith("/") || resolvedBody.startsWith("../")) {
            // probably a link to some other part of the site
            return;
          }
        }
        if (o instanceof HasAntoraReference) {
          AsciiDocFileReference file = ((HasAntoraReference) o).getAntoraReference();
          if (file != null) {
            ResolveResult[] resolveResults = file.multiResolve(false);
            if (resolveResults.length == 0) {
              // if the antora reference doesn't resolve, don't continue
              // as it might be a reference to an Antora component in another project
              continueResolving = false;
            }
          }
        }
        if (continueResolving && o instanceof HasFileReference) {
          AsciiDocFileReference file = ((HasFileReference) o).getFileReference();
          if (file != null) {
            ResolveResult[] resolveResults = file.multiResolve(false);
            if (resolveResults.length == 0) {
              LocalQuickFix[] fixes = new LocalQuickFix[]{};
              if (AsciiDocCreateMissingFile.isAvailable(o)) {
                fixes = new LocalQuickFix[]{CREATE_FILE};
              }
              holder.registerProblem(o, TEXT_HINT_FILE_DOESNT_RESOLVE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                file.getRangeInElement(), fixes);
              continueResolving = false;
            } else if (resolveResults.length > 1) {
              continueResolving = false;
            }
          }
        }
        if (continueResolving && o instanceof HasAnchorReference) {
          AsciiDocFileReference anchor = ((HasAnchorReference) o).getAnchorReference();
          if (anchor != null) {
            ResolveResult[] resolveResultsAnchor = anchor.multiResolve(false);
            // only present an error if the anchor's attributes uniquely resolve
            if (resolveResultsAnchor.length == 0 && AsciiDocUtil.resolveAttributes(o, anchor.getRangeInElement().substring(o.getText())) != null) {
              ResolveResult[] resolveResultsAnchorCaseInsensitive = anchor.multiResolveAnchor(true);
              LocalQuickFix[] fixes = new LocalQuickFix[]{};
              if (resolveResultsAnchorCaseInsensitive.length == 1) {
                fixes = new LocalQuickFix[]{CHANGE_CASE_FOR_ANCHOR};
              }
              holder.registerProblem(o, TEXT_HINT_ANCHOR_DOESNT_RESOLVE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, anchor.getRangeInElement(), fixes);
            }
          }
        }
        super.visitElement(o);
      }
    };
  }
}

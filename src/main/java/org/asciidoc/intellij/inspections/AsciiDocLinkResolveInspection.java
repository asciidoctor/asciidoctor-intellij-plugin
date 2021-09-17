package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.asciidoc.intellij.psi.AsciiDocAttributeInBrackets;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocInlineMacro;
import org.asciidoc.intellij.psi.AsciiDocLink;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.psi.AttributeDeclaration;
import org.asciidoc.intellij.psi.HasAnchorReference;
import org.asciidoc.intellij.psi.HasAntoraReference;
import org.asciidoc.intellij.psi.HasFileReference;
import org.asciidoc.intellij.quickfix.AsciiDocChangeCaseForAnchor;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFile;
import org.asciidoc.intellij.quickfix.AsciiDocCreateMissingFileQuickfix;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocLinkResolveInspection extends AsciiDocInspectionBase {
  private static final @InspectionMessage String TEXT_HINT_FILE_DOESNT_RESOLVE = "File doesn't resolve";
  private static final @InspectionMessage String TEXT_HINT_ANCHOR_DOESNT_RESOLVE = "Anchor doesn't resolve";
  private static final @InspectionMessage String TEXT_HINT_FILE_USES_DIFFERENT_CAPITALIZATION = "File name has different capitalization in document and on file system.";
  private static final @InspectionMessage String TEXT_HINT_DIRECTORY_USES_DIFFERENT_CAPITALIZATION = "Directory name has different capitalization in document and on file system.";
  private static final AsciiDocChangeCaseForAnchor CHANGE_CASE_FOR_ANCHOR = new AsciiDocChangeCaseForAnchor();
  private static final AsciiDocCreateMissingFileQuickfix CREATE_FILE = new AsciiDocCreateMissingFileQuickfix();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement o) {
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
          if (blockMacro.getMacroName().equals("image") || blockMacro.getMacroName().equals("video")) {
            continueResolving = hasImagesDirAsUrl(o);
          }
        }
        if (o instanceof AsciiDocInlineMacro) {
          if (((AsciiDocInlineMacro) o).getMacroName().equals("image")) {
            continueResolving = hasImagesDirAsUrl(o);
          }
        }
        if (continueResolving &&
          (o instanceof AsciiDocLink || o instanceof AsciiDocBlockMacro || o instanceof AsciiDocInlineMacro)) {
          String resolvedBody;
          String macroName = "";
          if (o instanceof AsciiDocLink) {
            resolvedBody = ((AsciiDocLink) o).getResolvedBody();
          } else if (o instanceof AsciiDocBlockMacro) {
            resolvedBody = ((AsciiDocBlockMacro) o).getResolvedBody();
            macroName = ((AsciiDocBlockMacro) o).getMacroName();
          } else {
            resolvedBody = ((AsciiDocInlineMacro) o).getResolvedBody();
            macroName = ((AsciiDocInlineMacro) o).getMacroName();
          }
          if (resolvedBody == null) {
            return;
          } else if (AsciiDocUtil.URL_PREFIX_PATTERN.matcher(resolvedBody).find()) {
            // this is a URL, don't
            return;
          } else if (o instanceof AsciiDocLink && ((AsciiDocLink) o).getMacroName().equals("link") && resolvedBody.startsWith("about:")) {
            // this is a special about: URL, don't report this as an error
            return;
          } else if (AsciiDocFileReference.URL.matcher(resolvedBody).find() && (macroName.equals("image") || macroName.equals("video"))) {
            // this is a data URI for an image, don't
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
              // if the Antora reference doesn't resolve, don't continue
              // as it might be a reference to an Antora component in another project
              continueResolving = false;
            }
          }
        }
        // check for file to resolve
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
        if (continueResolving) {
          continueResolving = checkCapitalizationOfFiles(o, holder);
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

  /**
   * Check for capitalization of file or directory names.
   * This check will only work on an OS that doesn't treat file names as case-sensitive (that is: MS Windows).
   * Therefore there is no automated test for this.
   */
  private boolean checkCapitalizationOfFiles(@NotNull PsiElement o, @NotNull ProblemsHolder holder) {
    boolean continueResolving = true;
    for (PsiReference reference : o.getReferences()) {
      if (reference instanceof AsciiDocFileReference) {
        AsciiDocFileReference fileReference = (AsciiDocFileReference) reference;
        if (fileReference.isAnchor() || fileReference.isAntora()) {
          continue;
        }
        ResolveResult[] resolveResults = fileReference.multiResolve(false);
        if (resolveResults.length == 1) {
          if (resolveResults[0].getElement() instanceof PsiFileSystemItem) {
            PsiFileSystemItem resolvedFile = (PsiFileSystemItem) resolveResults[0].getElement();
            if (resolvedFile.getVirtualFile().getFileSystem().isCaseSensitive()) {
              // this will only happen if the user is on a case-insensitive file system, no need to continue
              continue;
            }
            String refText = fileReference.getRangeInElement().substring(o.getText());
            String refTextResolved = AsciiDocUtil.resolveAttributes(o, refText);
            if (refTextResolved != null) {
              refText = refTextResolved;
            }
            if (!Objects.equals(refText, resolvedFile.getName()) && Objects.equals(refText.toLowerCase(Locale.US), resolvedFile.getName().toLowerCase(Locale.US))) {
              String diff1 = StringUtils.difference(refText, resolvedFile.getName());
              diff1 = StringEscapeUtils.escapeHtml4(diff1);
              String diff2 = StringUtils.difference(resolvedFile.getName(), refText);
              diff2 = StringEscapeUtils.escapeHtml4(diff2);
              boolean isAntora = AsciiDocUtil.findAntoraModuleDir(o) != null;
              holder.registerProblem(o,
                "<html>" +
                (resolvedFile.isDirectory() ? TEXT_HINT_DIRECTORY_USES_DIFFERENT_CAPITALIZATION : TEXT_HINT_FILE_USES_DIFFERENT_CAPITALIZATION)
                + "<br>" + (isAntora ? "Resolving will fail when rendered with Antora." : "Resolving will fail when rendered on a case-sensitive file system (for example Linux).")
                + "<br>Difference is: <code>" + diff2 + "<code> vs. <code>" + diff1 + "</code>" +
                (diff1.length() != refText.length() ? " in <code>" + StringEscapeUtils.escapeHtml4(refText) + "</code>" : "") +
                "</html>",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                fileReference.getRangeInElement());
              continueResolving = false;
            }
          }
        }
      }
    }
    return continueResolving;
  }

  private boolean hasImagesDirAsUrl(PsiElement o) {
    boolean continueResolving = true;
    List<AttributeDeclaration> imagesdirs = AsciiDocUtil.findAttributes(o.getProject(), "imagesdir");
    // if there is an imagesdir declaration in the same file before the image, and it contains a URL, don't continue
    for (AttributeDeclaration d : imagesdirs) {
      if (!(d instanceof AsciiDocAttributeDeclaration)) {
        continue;
      }
      AsciiDocAttributeDeclaration decl = (AsciiDocAttributeDeclaration) d;
      String val = decl.getAttributeValue();
      if (decl.getContainingFile().equals(o.getContainingFile()) &&
        decl.getTextOffset() < o.getTextOffset() &&
        val != null &&
        AsciiDocFileReference.URL.matcher(val).find()) {
        continueResolving = false;
        break;
      }
    }
    return continueResolving;
  }
}

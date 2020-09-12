package org.asciidoc.intellij.psi;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class PageAttributeProcessor implements PsiElementProcessor<PsiElement> {
  private final Collection<AsciiDocAttributeDeclaration> result;
  private final int depth;
  private boolean titleSeen = false;

  public PageAttributeProcessor(Collection<AsciiDocAttributeDeclaration> result, int depth) {
    this.result = result;
    this.depth = depth;
  }

  @Override
  public boolean execute(@NotNull PsiElement element) {
    if (element instanceof PsiFile) {
      return true;
    }
    if (element instanceof AsciiDocAttributeDeclaration) {
      result.add((AsciiDocAttributeDeclaration) element);
      return true;
    }
    if (PsiTreeUtil.getParentOfType(element, AsciiDocAttributeDeclaration.class) != null) {
      return true;
    }
    if (PsiTreeUtil.getParentOfType(element, AsciiDocBlockAttributes.class, false) != null) {
      return true;
    }
    if (PsiTreeUtil.getParentOfType(element, AsciiDocBlockId.class, false) != null) {
      return true;
    }
    if (element instanceof AsciiDocBlockMacro) {
      AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) element;
      if (blockMacro.getMacroName().equals("include")) {
        AsciiDocFileReference fileReference = blockMacro.getFileReference();
        if (fileReference != null) {
          PsiElement resolved = fileReference.resolve();
          if (resolved != null) {
            return AsciiDocUtil.findPageAttributes((PsiFile) resolved, depth + 1, result);
          }
        }
        return true;
      }
    }
    if (element instanceof AsciiDocSection) {
      if (((AsciiDocSection) element).getHeadingLevel() == 1) {
        titleSeen = true;
        return true;
      }
    } else if (titleSeen && element.getNode().getElementType() == AsciiDocTokenTypes.EMPTY_LINE) {
      return false;
    } else if (element.getNode().getElementType() == AsciiDocTokenTypes.HEADING_TOKEN) {
      return true;
    } else if (element.getNode().getElementType() == AsciiDocTokenTypes.HEADER) {
      return true;
    } else if (element.getNode().getElementType() == AsciiDocTokenTypes.BLOCKIDSTART) {
      return true;
    } else if (element.getNode().getElementType() == AsciiDocTokenTypes.BLOCKIDEND) {
      return true;
    } else if (element.getNode().getElementType() == AsciiDocTokenTypes.HEADING_OLDSTYLE) {
      return true;
    } else if (element instanceof PsiWhiteSpace) {
      return true;
    } else //noinspection RedundantIfStatement
      if (element instanceof PsiComment) {
      return true;
    }
    return false;
  }
}

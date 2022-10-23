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
  private final Collection<AttributeDeclaration> result;
  private final int depth;
  private final PsiFile root;
  private boolean titleSeen = false;

  public PageAttributeProcessor(PsiFile root, Collection<AttributeDeclaration> result, int depth) {
    this.root = root;
    this.result = result;
    this.depth = depth;
  }

  @Override
  public boolean execute(@NotNull PsiElement element) {
    if (element instanceof PsiFile) {
      return true;
    }
    if (element instanceof AsciiDocAttributeDeclaration) {
      // last attribute definition should take precedence
      AsciiDocAttributeDeclaration attributeDeclaration = (AsciiDocAttributeDeclaration) element;
      if (attributeDeclaration.getAttributeName().equals("doctitle")) {
        result.removeIf(ad -> ad.getAttributeName().equals("doctitle"));
      }
      result.add(attributeDeclaration);
      return true;
    }
    if (element instanceof AsciiDocFrontmatter || PsiTreeUtil.getParentOfType(element, AsciiDocFrontmatter.class) != null) {
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
          // map file reference to included file to original file for resolving
          fileReference = new AsciiDocFileReference(fileReference, root);
          PsiElement resolved = fileReference.resolve();
          if (resolved != null) {
            return AsciiDocUtil.findPageAttributes((PsiFile) resolved, root, depth + 1, result);
          }
        }
        return true;
      }
    }
    if (PsiTreeUtil.getParentOfType(element, AsciiDocHeading.class, false) != null) {
      return true;
    }
    if (element instanceof AsciiDocSection) {
      if (((AsciiDocSection) element).getHeadingLevel() == 1) {
        // only allow doctitle to be taken from heading if not already set
        if (result.stream().noneMatch(attributeDeclaration -> attributeDeclaration.getAttributeName().equals("doctitle"))) {
          result.add(new AsciiDocAttributeDeclarationDummy("doctitle", ((AsciiDocSection) element).getTitle()));
        }
        titleSeen = true;
        return true;
      }
    } else if (titleSeen && element.getNode() != null && element.getNode().getElementType() == AsciiDocTokenTypes.EMPTY_LINE) {
      return false;
    } else if (element.getNode() != null && element.getNode().getElementType() == AsciiDocTokenTypes.HEADER) {
      return true;
    } else if (element.getNode() != null && element.getNode().getElementType() == AsciiDocTokenTypes.BLOCKIDSTART) {
      return true;
    } else if (element.getNode() != null && element.getNode().getElementType() == AsciiDocTokenTypes.BLOCKIDEND) {
      return true;
    } else if (element.getNode() != null && element.getNode().getElementType() == AsciiDocTokenTypes.HEADING_OLDSTYLE) {
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

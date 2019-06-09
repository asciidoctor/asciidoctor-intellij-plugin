package org.asciidoc.intellij.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocSelfDescribe;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AsciiDocFoldingBuilder extends CustomFoldingBuilder implements DumbAware {
  @Override
  protected void buildLanguageFoldRegions(@NotNull List<FoldingDescriptor> descriptors,
                                          @NotNull PsiElement root,
                                          @NotNull Document document,
                                          boolean quick) {
    root.accept(new AsciiDocVisitor() {

      @Override
      public void visitElement(PsiElement element) {
        super.visitElement(element);
        element.acceptChildren(this);
      }

      @Override
      public void visitSections(@NotNull AsciiDocSection section) {
        addDescriptors(section);
        super.visitSections(section);
      }

      @Override
      public void visitBlocks(@NotNull AsciiDocBlock block) {
        addDescriptors(block);
        super.visitBlocks(block);
      }

      private void addDescriptors(@NotNull PsiElement element) {
        AsciiDocFoldingBuilder.addDescriptors(element, element.getTextRange(), descriptors, document);
      }
    });

    root.accept(new AsciiDocVisitor() {
      @Override
      public void visitSections(@NotNull AsciiDocSection header) {
        super.visitSections(header);
      }

      @Override
      public void visitBlocks(@NotNull AsciiDocBlock block) {
        super.visitBlocks(block);
      }
    });
  }

  private static void addDescriptors(@NotNull PsiElement element,
                                     @NotNull TextRange range,
                                     @NotNull List<? super FoldingDescriptor> descriptors,
                                     @NotNull Document document) {
    if (document.getLineNumber(range.getStartOffset()) != document.getLineNumber(range.getEndOffset() - 1)) {
      descriptors.add(new FoldingDescriptor(element, range));
    }
  }

  @Override
  protected String getLanguagePlaceholderText(@NotNull ASTNode node, @NotNull TextRange range) {
    String title;
    if (node.getPsi() instanceof AsciiDocSelfDescribe) {
      title = ((AsciiDocSelfDescribe) node.getPsi()).getFoldedSummary();
      title = StringUtil.shortenTextWithEllipsis(title, 50, 5);
      title += " ...";
    } else {
      title = StringUtil.shortenTextWithEllipsis(node.getText(), 50, 5);
    }
    return title;
  }

  @Override
  protected boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
    return false;
  }
}

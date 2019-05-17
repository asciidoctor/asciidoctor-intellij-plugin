package org.asciidoc.intellij.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsciiDocFoldingBuilder extends CustomFoldingBuilder implements DumbAware {
  private static final Map<IElementType, String> TYPES_PRESENTATION_MAP = new HashMap<>();

  static {
    TYPES_PRESENTATION_MAP.put(AsciiDocElementTypes.SECTION, AsciiDocBundle.message("asciidoc.folding.section.name"));
    TYPES_PRESENTATION_MAP.put(AsciiDocElementTypes.BLOCK, AsciiDocBundle.message("asciidoc.folding.block.name"));
  }

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
    IElementType elementType = PsiUtilCore.getElementType(node);
    String title = "";
    String explicitName = TYPES_PRESENTATION_MAP.get(elementType);
    final String prefix = explicitName != null ? explicitName + ": " : "";
    if (node.getPsi() instanceof AsciiDocSection) {
      title = ((AsciiDocSection) node.getPsi()).getTitle();
      if (title != null && title.length() > 0) {
        title = prefix + StringUtil.shortenTextWithEllipsis(title, 50, 5);
      }
    } else if (node.getPsi() instanceof AsciiDocBlock) {
      title = ((AsciiDocBlock) node.getPsi()).getTitle();
      if (title != null && title.length() > 0) {
        title = prefix + StringUtil.shortenTextWithEllipsis(title, 50, 5);
      }
    }
    if (title == null || title.length() == 0) {
      title = prefix + StringUtil.shortenTextWithEllipsis(node.getText(), 50, 5);
    }
    return title;
  }

  @Override
  protected boolean isRegionCollapsedByDefault(@NotNull ASTNode node) {
    return false;
  }
}

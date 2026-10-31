package org.asciidoc.intellij.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ASTNode;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.quickfix.AsciiDocConvertMarkdownListing;
import org.asciidoc.intellij.quickfix.AsciiDocConvertOpenBlockListing;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fatih Bozik
 */
public class AsciiDocListingStyleInspection extends AsciiDocInspectionBase {
  public static final String MARKDOWN_LISTING_BLOCK_DELIMITER = "```";
  private static final String TEXT_HINT_MARKDOWN = "Markdown style listing";
  private static final String TEXT_HINT_OPEN_BLOCK = "Listing in an open block, the conventional delimiter is ";

  private static final AsciiDocConvertMarkdownListing MARKDOWN_LISTING_QUICKFIX = new AsciiDocConvertMarkdownListing();
  private static final AsciiDocConvertOpenBlockListing OPEN_BLOCK_LISTING_QUICKFIX = new AsciiDocConvertOpenBlockListing();

  @NotNull
  @Override
  protected AsciiDocVisitor buildAsciiDocVisitor(@NotNull ProblemsHolder holder,
                                                 @NotNull LocalInspectionToolSession session) {
    return new AsciiDocVisitor() {
      @Override
      public void visitBlocks(@NotNull AsciiDocBlock block) {
        if (isMarkdownListingBlock(block)) {
          final LocalQuickFix[] fixes = new LocalQuickFix[]{MARKDOWN_LISTING_QUICKFIX};
          holder.registerProblem(block, TEXT_HINT_MARKDOWN, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, fixes);
        } else if (isOpenBlockListing(block)) {
          final LocalQuickFix[] fixes = new LocalQuickFix[]{OPEN_BLOCK_LISTING_QUICKFIX};
          holder.registerProblem(block, TEXT_HINT_OPEN_BLOCK + AsciiDocConvertOpenBlockListing.getTargetDelimiter(block), ProblemHighlightType.WEAK_WARNING, fixes);
        }
        super.visitBlocks(block);
      }
    };
  }

  private boolean isOpenBlockListing(AsciiDocBlock block) {
    if (block.getType() != AsciiDocBlock.Type.LISTING || block.getNode() == null) {
      return false;
    }
    ASTNode node = block.getNode().getFirstChildNode();
    while (node != null && node.getElementType() != AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER) {
      node = node.getTreeNext();
    }
    return node != null && node.getText().trim().equals("--");
  }

  private boolean isMarkdownListingBlock(AsciiDocBlock block) {
    if (block.getType() != AsciiDocBlock.Type.LISTING) {
      return false;
    }
    if (block.getNode() == null) {
      return false;
    }
    ASTNode node  = block.getNode().getFirstChildNode();
    while (node != null && node.getElementType() != AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER) {
      node = node.getTreeNext();
    }
    if (node == null) {
      return false;
    }
    return node.getText().startsWith(MARKDOWN_LISTING_BLOCK_DELIMITER);
  }
}

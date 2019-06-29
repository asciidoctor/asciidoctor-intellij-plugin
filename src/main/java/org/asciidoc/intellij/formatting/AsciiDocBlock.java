package org.asciidoc.intellij.formatting;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.formatter.common.AbstractBlock;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.settings.AsciiDocApplicationSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class AsciiDocBlock extends AbstractBlock {
  AsciiDocBlock(@NotNull ASTNode node) {
    super(node, null, Alignment.createAlignment());
  }

  @Override
  protected List<Block> buildChildren() {
    final List<Block> result = new ArrayList<>();
    if (!AsciiDocApplicationSettings.getInstance().getAsciiDocPreviewSettings().isEnabledFormatSource()) {
      return result;
    }

    ASTNode child = myNode.getFirstChildNode();
    while (child != null) {
      if (!(child instanceof PsiWhiteSpace)) {
        result.add(new AsciiDocBlock(child));
      }
      child = child.getTreeNext();
    }
    return result;
  }

  @Nullable
  @Override
  public Alignment getAlignment() {
    return super.getAlignment();
  }

  @Nullable
  @Override
  public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
    if (child1 == null) {
      return null;
    }
    // keep blank lines before and after comments as is
    if (isComment(child1) || isComment(child2)) {
      return Spacing.createSpacing(0, 999, 0, true, 999);
    }

    // keep blank lines after block start
    if (isBlockStart(child1)) {
      return Spacing.createSpacing(0, 999, 0, true, 999);
    }

    // no blank line after title and block attribute
    if (isTitle(child1) || isBlockAttribute(child1) || isBlockIdEnd(child1)) {
      return Spacing.createSpacing(0, 0, 1, true, 0);
    }

    // have one blank line before and after a heading
    if (isSection(child1) || isSection(child2)) {
      return Spacing.createSpacing(0, 0, 2, false, 0);
    }

    // have one at least blank line before each bullet or enumeration,
    // but not if previous line starts with one as well (special case compact single line enumerations)
    if ((isEnumeration(child2) || isBullet(child2)) && !lineStartsWithEnumeration(child1)) {
      return Spacing.createSpacing(0, 0, 2, false, 0);
    }

    // before and after a block have one blank line, but not with if there is an continuation ("+")
    if (isBlock(child2) && !isContinuation(child1) && !isBlock(child1)) {
      return Spacing.createSpacing(0, 0, 2, false, 0);
    }
    if (isBlock(child1) && !isContinuation(child2) && !isBlock(child2)) {
      return Spacing.createSpacing(0, 0, 2, false, 0);
    }

    return Spacing.createSpacing(0, 999, 0, true, 999);
  }

  private boolean lineStartsWithEnumeration(Block block) {
    ASTNode node = ((AsciiDocBlock) block).getNode().getTreePrev();
    while (node != null) {
      // linebreaks are now Whitespace, no type associated any more
      if ("\n".equals(node.getText())) {
        return false;
      }
      if (node.getElementType() == AsciiDocTokenTypes.BULLET
        || node.getElementType() == AsciiDocTokenTypes.ENUMERATION) {
        return true;
      }
      node = node.getTreePrev();
    }
    return false;
  }

  private boolean isBlockStart(Block block) {
    return block instanceof AsciiDocBlock &&
      (AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER.equals(((AsciiDocBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.BLOCK_DELIMITER.equals(((AsciiDocBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER.equals(((AsciiDocBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER.equals(((AsciiDocBlock) block).getNode().getElementType())
      );
  }

  private boolean isComment(Block block) {
    return block instanceof AsciiDocBlock &&
      (AsciiDocTokenTypes.COMMENT_BLOCK_DELIMITER.equals(((AsciiDocBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.LINE_COMMENT.equals(((AsciiDocBlock) block).getNode().getElementType()));
  }

  private boolean isBlockAttribute(Block block) {
    return block instanceof AsciiDocBlock &&
      AsciiDocElementTypes.BLOCK_ATTRIBUTES.equals(((AsciiDocBlock) block).getNode().getElementType());
  }

  private boolean isBlockIdEnd(Block block) {
    return block instanceof AsciiDocBlock &&
      AsciiDocTokenTypes.BLOCKIDEND.equals(((AsciiDocBlock) block).getNode().getElementType());
  }

  private boolean isBlock(Block block) {
    return block instanceof AsciiDocBlock &&
      (AsciiDocElementTypes.BLOCK.equals(((AsciiDocBlock) block).getNode().getElementType())
        || AsciiDocElementTypes.LISTING.equals(((AsciiDocBlock) block).getNode().getElementType()));
  }

  private boolean isContinuation(Block block) {
    return block instanceof AsciiDocBlock &&
      ((AsciiDocBlock) block).getNode().getText().equals("+");
  }

  private boolean isTitle(Block block) {
    return block instanceof AsciiDocBlock &&
      AsciiDocTokenTypes.TITLE.equals(((AsciiDocBlock) block).getNode().getElementType());
  }

  private static boolean isSection(Block block) {
    return block instanceof AsciiDocBlock &&
      (AsciiDocElementTypes.SECTION.equals(((AsciiDocBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.HEADING.equals(((AsciiDocBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.HEADING_OLDSTYLE.equals(((AsciiDocBlock) block).getNode().getElementType()));
  }

  private static boolean isBullet(Block block) {
    return block instanceof AsciiDocBlock &&
      AsciiDocTokenTypes.BULLET.equals(((AsciiDocBlock) block).getNode().getElementType());
  }

  private static boolean isEnumeration(Block block) {
    return block instanceof AsciiDocBlock &&
      AsciiDocTokenTypes.ENUMERATION.equals(((AsciiDocBlock) block).getNode().getElementType());
  }

  @Override
  public Indent getIndent() {
    ASTNode treePrev = myNode.getTreePrev();
    if (treePrev instanceof PsiWhiteSpace) {
      int spaces = 0;
      char[] chars = ((PsiWhiteSpace) treePrev).textToCharArray();
      int i = treePrev.getTextLength() - 1;
      for (; i >= 0; --i) {
        if (chars[i] == ' ') {
          spaces++;
        } else {
          break;
        }
      }
      if (i < 0 || chars[i] == '\n') {
        return Indent.getSpaceIndent(spaces);
      }
    }
    return Indent.getNoneIndent();
  }

  @Override
  public boolean isLeaf() {
    return getSubBlocks().size() == 0;
  }
}

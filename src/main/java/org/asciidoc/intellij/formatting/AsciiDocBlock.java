package org.asciidoc.intellij.formatting;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.codeStyle.AsciiDocCodeStyleSettings;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.psi.AsciiDocBlock.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

class AsciiDocBlock extends AbstractBlock {

  private boolean verse = false;
  private boolean table = false;
  private boolean hardbreaks = false;
  private final CodeStyleSettings settings;

  AsciiDocBlock(@NotNull ASTNode node, CodeStyleSettings settings) {
    super(node, null, Alignment.createAlignment());
    this.settings = settings;
  }

  private AsciiDocBlock(@NotNull ASTNode node, CodeStyleSettings settings, boolean verse, boolean table, boolean hardbreaks) {
    super(node, null, Alignment.createAlignment());
    this.settings = settings;
    this.verse = verse;
    this.table = table;
    this.hardbreaks = hardbreaks;
  }

  @Override
  protected List<Block> buildChildren() {
    final List<Block> result = new ArrayList<>();
    if (!settings.getCustomSettings(AsciiDocCodeStyleSettings.class).FORMATTING_ENABLED) {
      return result;
    }

    if (myNode.getPsi() instanceof org.asciidoc.intellij.psi.AsciiDocBlock) {
      // if this is inside a verse, pass this information down to all children
      org.asciidoc.intellij.psi.AsciiDocBlock block = (org.asciidoc.intellij.psi.AsciiDocBlock) myNode.getPsi();
      if (block.getType() == Type.VERSE) {
        verse = true;
      }
      if (block.getType() == Type.TABLE) {
        table = true;
      }
      if ("%hardbreaks".equals(block.getStyle())) {
        hardbreaks = true;
      }
    }

    ASTNode child = myNode.getFirstChildNode();
    while (child != null) {
      if (!(child instanceof PsiWhiteSpace)) {
        result.add(new AsciiDocBlock(child, settings, verse, table, hardbreaks));
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
    if (!verse && !table && (isTitle(child1) || isBlockAttribute(child1) || isBlockIdEnd(child1))) {
      return Spacing.createSpacing(0, 0, 1, true, 0);
    }

    // have one blank line before and after a heading
    if (!verse && !table && ((isSection(child1) && !isHeader(child2)) || isSection(child2))) {
      return Spacing.createSpacing(0, 0, 2, false, 0);
    }

    if (settings.getCustomSettings(AsciiDocCodeStyleSettings.class).ONE_SENTENCE_PER_LINE) {
      // ensure a new line at the end of the sentence
      if (!verse && !table && !hardbreaks && isEndOfSentence(child1) && isPartOfSentence(child2)) {
        return Spacing.createSpacing(0, 0, 1, true, 1);
      }

      // ensure exactly one space between parts of one sentence. Remove any newlines
      if (!verse && !table && !hardbreaks && isPartOfSentence(child1) && isPartOfSentence(child2) && !hasBlankLineBetween(child1, child2)) {
        // if there is a newline, create at least one space
        int minSpaces = hasNewlinesBetween(child1, child2) ? 1 : 0;
        return Spacing.createSpacing(minSpaces, 1, 0, false, 0);
      }
    }

    // have one at least blank line before each bullet or enumeration,
    // but not if previous line starts with one as well (special case compact single line enumerations)
    /* disabled, as it only tackles single enumeration items
    if (!verse && !table && ((isEnumeration(child2) || isBullet(child2)) && !lineStartsWithEnumeration(child1) && !isContinuation(child1))) {
      return Spacing.createSpacing(0, 0, 2, false, 0);
    }
    */

    // one space after enumeration or bullet
    if (isEnumeration(child1) || isBullet(child1)) {
      return Spacing.createSpacing(1, 1, 0, false, 0);
    }

    // no space before or after separator in block attributes
    if (isSeparator(child1) || isSeparator(child2)) {
      return Spacing.createSpacing(0, 0, 0, false, 0);
    }

    // before and after a block have one blank line, but not with if there is an continuation ("+")
    if (!table && isBlock(child2) && !isContinuation(child1) && !isBlockStart(child1)) {
      return Spacing.createSpacing(0, 0, 2, false, 0);
    }
    if (!table && isBlock(child1) && !isContinuation(child2) && !isBlockEnd(child2) && !isCallOut(child2)) {
      return Spacing.createSpacing(0, 0, 2, false, 0);
    }

    return Spacing.createSpacing(0, 999, 0, true, 999);
  }

  private boolean isHeader(Block block) {
    return block instanceof AsciiDocBlock &&
      (AsciiDocTokenTypes.HEADER.equals(((AsciiDocBlock) block).getNode().getElementType()));
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
      ) &&
      ((AsciiDocBlock) block).getNode().getTreeNext() != null;
  }

  private boolean isBlockEnd(Block block) {
    return block instanceof AsciiDocBlock &&
      (AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER.equals(((AsciiDocBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.BLOCK_DELIMITER.equals(((AsciiDocBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER.equals(((AsciiDocBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER.equals(((AsciiDocBlock) block).getNode().getElementType())
      ) &&
      ((AsciiDocBlock) block).getNode().getTreeNext() == null;
  }

  private boolean isSeparator(Block block) {
    return block instanceof AsciiDocBlock &&
      (AsciiDocTokenTypes.SEPARATOR.equals(((AsciiDocBlock) block).getNode().getElementType()));
  }

  private boolean hasBlankLineBetween(Block child1, Block child2) {
    if (!(child1 instanceof AsciiDocBlock)) {
      return false;
    }
    if (!(child2 instanceof AsciiDocBlock)) {
      return false;
    }
    int newlines = 0;
    ASTNode node = ((AsciiDocBlock) child1).getNode().getTreeNext();
    while (node != null && node != ((AsciiDocBlock) child2).getNode()) {
      if (node instanceof PsiWhiteSpace && "\n".equals(node.getText())) {
        newlines++;
        if (newlines == 2) {
          return true;
        }
      }
      if (!(node instanceof PsiWhiteSpace)) {
        return false;
      }
      node = node.getTreeNext();
    }
    return false;
  }

  private boolean hasNewlinesBetween(Block child1, Block child2) {
    if (!(child1 instanceof AsciiDocBlock)) {
      return false;
    }
    if (!(child2 instanceof AsciiDocBlock)) {
      return false;
    }
    ASTNode node = ((AsciiDocBlock) child1).getNode().getTreeNext();
    while (node != null && node != ((AsciiDocBlock) child2).getNode()) {
      if (node instanceof PsiWhiteSpace && "\n".equals(node.getText())) {
        return true;
      }
      node = node.getTreeNext();
    }
    return false;
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

  private boolean isBlockIdStart(Block block) {
    return block instanceof AsciiDocBlock &&
      AsciiDocTokenTypes.BLOCKIDSTART.equals(((AsciiDocBlock) block).getNode().getElementType());
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

  private boolean isEndOfSentence(Block block) {
    return block instanceof AsciiDocBlock &&
      AsciiDocTokenTypes.END_OF_SENTENCE.equals(((AsciiDocBlock) block).getNode().getElementType());
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

  private boolean isCallOut(Block block) {
    return block instanceof AsciiDocBlock &&
      AsciiDocTokenTypes.CALLOUT.equals(((AsciiDocBlock) block).getNode().getElementType());
  }

  private static final TokenSet TEXT_SET = TokenSet.create(AsciiDocTokenTypes.TEXT, AsciiDocTokenTypes.BOLD, AsciiDocTokenTypes.BOLDITALIC,
    AsciiDocTokenTypes.ITALIC, AsciiDocTokenTypes.DOUBLE_QUOTE, AsciiDocTokenTypes.SINGLE_QUOTE, AsciiDocTokenTypes.BOLD_START,
    AsciiDocTokenTypes.BOLD_END, AsciiDocTokenTypes.ITALIC_START, AsciiDocTokenTypes.ITALIC_END, AsciiDocTokenTypes.LT,
    AsciiDocTokenTypes.GT, AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_END, AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_START,
    AsciiDocTokenTypes.LPAREN, AsciiDocTokenTypes.RPAREN,
    AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_END, AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START);

  private static boolean isPartOfSentence(Block block) {
    return block instanceof AsciiDocBlock &&
      TEXT_SET.contains(((AsciiDocBlock) block).getNode().getElementType()) &&
      !"::".equals(((AsciiDocBlock) block).getNode().getText()) && // should stay on a separate line as reformatting might create property list item
      !"--".equals(((AsciiDocBlock) block).getNode().getText()); // should stay on a separate line as it might be part of a quote
  }

  @Override
  public Indent getIndent() {
    if (myNode.getElementType() == AsciiDocTokenTypes.ENUMERATION
      || myNode.getElementType() == AsciiDocTokenTypes.BULLET
      || myNode.getElementType() == AsciiDocTokenTypes.DESCRIPTION
      || myNode.getElementType() == AsciiDocElementTypes.LINK
      || myNode.getElementType() == AsciiDocElementTypes.ATTRIBUTE_REF) {
      return Indent.getSpaceIndent(0);
    }

    if (!verse && TEXT_SET.contains(myNode.getElementType())) {
      return Indent.getSpaceIndent(0);
    }

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

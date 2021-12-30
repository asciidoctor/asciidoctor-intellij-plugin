package org.asciidoc.intellij.formatting;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.WhiteSpaceFormattingStrategy;
import com.intellij.psi.formatter.WhiteSpaceFormattingStrategyFactory;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.codeStyle.AsciiDocCodeStyleSettings;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocPsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AsciiDocFormattingBlock extends AbstractBlock {
  private boolean verse = false;
  private boolean table = false;
  private boolean hardbreaks = false;
  private final CodeStyleSettings settings;
  private final Map<String, WhiteSpaceFormattingStrategy> wssCache;

  AsciiDocFormattingBlock(@NotNull ASTNode node, CodeStyleSettings settings) {
    super(node, null, Alignment.createAlignment());
    this.settings = settings;
    this.wssCache = new HashMap<>();
  }

  private AsciiDocFormattingBlock(@NotNull ASTNode node, CodeStyleSettings settings, boolean verse, boolean table, boolean hardbreaks, Map<String, WhiteSpaceFormattingStrategy> wss, Alignment alignment) {
    super(node, null, alignment);
    this.settings = settings;
    this.verse = verse;
    this.table = table;
    this.hardbreaks = hardbreaks;
    this.wssCache = wss;
  }

  @Override
  protected List<Block> buildChildren() {
    final List<Block> result = new ArrayList<>();
    if (!settings.getCustomSettings(AsciiDocCodeStyleSettings.class).FORMATTING_ENABLED) {
      return result;
    }

    if (myNode.getPsi() instanceof org.asciidoc.intellij.psi.AsciiDocBlock) {
      // if this is inside a verse, pass this information down to all children
      AsciiDocBlock block = (AsciiDocBlock) myNode.getPsi();
      if (block.getType() == AsciiDocBlock.Type.VERSE) {
        verse = true;
      }
      if (block.getType() == AsciiDocBlock.Type.TABLE) {
        table = true;
      }
      if ("%hardbreaks".equals(block.getStyle())) {
        hardbreaks = true;
      }
    }

    ASTNode child = myNode.getFirstChildNode();
    while (child != null) {
      if (!(child instanceof PsiWhiteSpace)) {
        // every child will align with the the parent, no additional indents due to alignment
        // as leading blanks in Asciidoc in a line can either change the meaning
        // verse blocks with have their own alignment so that they can add spaces as needed to the beginning of the line
        result.add(new AsciiDocFormattingBlock(child, settings, verse, table, hardbreaks, wssCache, verse ? Alignment.createAlignment() : getAlignment()));
      } else {
        Language language = ((PsiWhiteSpace) child).getLanguage();
        WhiteSpaceFormattingStrategy myWhiteSpaceStrategy = wssCache.computeIfAbsent(language.getID(),
          s -> WhiteSpaceFormattingStrategyFactory.getStrategy(language));
        // double-check for a whitespace problem in lexer before re-formatting,
        // otherwise non-whitespace characters might get lost!
        CharSequence text = child.getChars();
        int end = text.length();
        if (myWhiteSpaceStrategy.check(text, 0, end) != end) {
          // reason for this to happen: a missing "return" in lexer for the first non-whitespace character
          StringBuilder tree = new StringBuilder(childToString(child));
          ASTNode node = child.getTreeParent();
          while (node != null) {
            tree.insert(0, childToString(node) + " > ");
            node = node.getTreeParent();
          }
          throw AsciiDocPsiImplUtil.getRuntimeException("Whitespace element contains non-whitespace-characters at offset " + child.getStartOffset() + ", tree: " + tree,
            child.getText(), null);
        }
      }
      child = child.getTreeNext();
    }
    return result;
  }

  @NotNull
  private String childToString(ASTNode child) {
    return child.getElementType() + ":" + child.getPsi().getLanguage().getDisplayName();
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
    if (isTitleInsideTitle(child1) && !isTitleInsideTitle(child2)) {
      return Spacing.createSpacing(0, 0, 1, true, 0);
    }

    // no blank line after title and block attribute
    if (!verse && !table && (isBlockAttribute(child1) || isBlockIdEnd(child1))) {
      return Spacing.createSpacing(0, 0, 1, true, 0);
    }

    // blank line(s) before and after a heading
    if (!verse && !table && (isSection(child1) && !isPartOfSameHeading(child1, child2) && !isAttributeDeclaration(child2) && !isHeader(child2))) {
      int minBlankLinesAfterHeader = settings.getCustomSettings(AsciiDocCodeStyleSettings.class).BLANK_LINES_AFTER_HEADER;
      int maxBlankLinesAfterHeader = settings.getCustomSettings(AsciiDocCodeStyleSettings.class).BLANK_LINES_KEEP_AFTER_HEADER;
      return Spacing.createSpacing(0, 0, minBlankLinesAfterHeader + 1, true, maxBlankLinesAfterHeader);
    }

    if (settings.getCustomSettings(AsciiDocCodeStyleSettings.class).ONE_SENTENCE_PER_LINE) {
      // ensure a new line at the end of the sentence
      if (!verse && !table && !hardbreaks && isEndOfSentence(child1) && isPartOfSentence(child2)
        && !isTitleInsideTitle(child1)) {
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

    // if a block starts within a cell, start a new line for the block
    if (isCellStart(child1) && isBlock(child2)) {
      return Spacing.createSpacing(0, 0, 1, false, 0);
    }

    // before and after a block have one blank line, but not with if there is a continuation ("+"), or if it is a preprocessor macro
    if (!table && isBlock(child2) && !isContinuation(child1) && !isBlockStart(child1)) {
      return getSpacingForBlocks(child1, child2);
    }
    if (!table && isBlock(child1) && !isContinuation(child2) && !isBlockEnd(child2) && !isCallOut(child2)) {
      return getSpacingForBlocks(child1, child2);
    }

    return Spacing.createSpacing(0, 999, 0, true, 999);
  }

  private Spacing getSpacingForBlocks(@NotNull Block child1, @NotNull Block child2) {
    if (isPreprocessorMacro(child1) || isPreprocessorMacro(child2)) {
      // have zero or one line before and after a preprocessor macro;
      // this shouldn't add a blank line when there is no blank line
      return Spacing.createSpacing(0, 0, 1, true, 1);
    } else {
      // have one blank line between blocks
      return Spacing.createSpacing(0, 0, 2, false, 0);
    }
  }

  private boolean isPartOfSameHeading(Block child1, Block child2) {
    ASTNode node1 = ((AsciiDocFormattingBlock) child2).getNode();
    ASTNode node2 = ((AsciiDocFormattingBlock) child1).getNode();
    node1 = getHeadingFor(node1);
    node2 = getHeadingFor(node2);
    return node1 != null && node1 == node2;
  }

  private ASTNode getHeadingFor(ASTNode node) {
    do {
      if (node.getElementType() == AsciiDocElementTypes.HEADING) {
        break;
      }
      node = node.getTreeParent();
    } while (node != null);
    return node;
  }

  private boolean isCellStart(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      AsciiDocTokenTypes.CELLSEPARATOR.equals(((AsciiDocFormattingBlock) block).getNode().getElementType());
  }

  private static boolean isAttributeDeclaration(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      AsciiDocElementTypes.ATTRIBUTE_DECLARATION.equals(((AsciiDocFormattingBlock) block).getNode().getElementType());
  }

  private boolean isBlockStart(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      (AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.BLOCK_DELIMITER.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
      ) &&
      ((AsciiDocFormattingBlock) block).getNode().getTreeNext() != null;
  }

  private boolean isBlockEnd(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      (AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.BLOCK_DELIMITER.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.PASSTRHOUGH_BLOCK_DELIMITER.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
      ) &&
      ((AsciiDocFormattingBlock) block).getNode().getTreeNext() == null;
  }

  private boolean isSeparator(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      AsciiDocTokenTypes.SEPARATOR.equals(((AsciiDocFormattingBlock) block).getNode().getElementType());
  }

  private boolean hasBlankLineBetween(Block child1, Block child2) {
    if (!(child1 instanceof AsciiDocFormattingBlock)) {
      return false;
    }
    if (!(child2 instanceof AsciiDocFormattingBlock)) {
      return false;
    }
    int newlines = 0;
    ASTNode node = ((AsciiDocFormattingBlock) child1).getNode().getTreeNext();
    while (node != null && node != ((AsciiDocFormattingBlock) child2).getNode()) {
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
    if (!(child1 instanceof AsciiDocFormattingBlock)) {
      return false;
    }
    if (!(child2 instanceof AsciiDocFormattingBlock)) {
      return false;
    }
    ASTNode node = ((AsciiDocFormattingBlock) child1).getNode().getTreeNext();
    while (node != null && node != ((AsciiDocFormattingBlock) child2).getNode()) {
      if (node instanceof PsiWhiteSpace && "\n".equals(node.getText())) {
        return true;
      }
      node = node.getTreeNext();
    }
    return false;
  }

  private boolean isComment(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      (AsciiDocTokenTypes.COMMENT_BLOCK_DELIMITER.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
        || AsciiDocTokenTypes.LINE_COMMENT.equals(((AsciiDocFormattingBlock) block).getNode().getElementType()));
  }

  private boolean isBlockAttribute(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      AsciiDocElementTypes.BLOCK_ATTRIBUTES.equals(((AsciiDocFormattingBlock) block).getNode().getElementType());
  }

  private boolean isBlockIdEnd(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      AsciiDocTokenTypes.BLOCKIDEND.equals(((AsciiDocFormattingBlock) block).getNode().getElementType());
  }

  private boolean isPreprocessorMacro(Block block) {
    if (((AsciiDocFormattingBlock) block).getNode().getPsi() instanceof AsciiDocBlockMacro) {
      AsciiDocBlockMacro blockMacro = (AsciiDocBlockMacro) ((AsciiDocFormattingBlock) block).getNode().getPsi();
      return blockMacro.isPreprocessorMacro();
    }
    return false;
  }

  private boolean isBlock(Block block) {
    if (block.getSubBlocks().size() > 0) {
      IElementType elementType = ((AsciiDocFormattingBlock) block.getSubBlocks().get(0)).getNode().getElementType();
      if (elementType == AsciiDocTokenTypes.ENUMERATION || elementType == AsciiDocTokenTypes.BULLET || elementType == AsciiDocTokenTypes.DESCRIPTION || elementType == AsciiDocTokenTypes.DESCRIPTION_END || elementType == AsciiDocTokenTypes.CALLOUT) {
        // these are all dummy blocks to help with folding and spell checking,
        // therefore don't treat as regular block that get new lines before and after
        return false;
      }
    }
    return block instanceof AsciiDocFormattingBlock &&
      (AsciiDocElementTypes.BLOCK.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
        || AsciiDocElementTypes.LISTING.equals(((AsciiDocFormattingBlock) block).getNode().getElementType()));
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isContinuation(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      ((AsciiDocFormattingBlock) block).getNode().getText().equals("+");
  }

  private boolean isEndOfSentence(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      AsciiDocTokenTypes.END_OF_SENTENCE.equals(((AsciiDocFormattingBlock) block).getNode().getElementType());
  }

  private boolean isTitleInsideTitle(Block block) {
    if (block instanceof AsciiDocFormattingBlock) {
      AsciiDocFormattingBlock adBlock = (AsciiDocFormattingBlock) block;
      ASTNode node = adBlock.getNode();
      do {
        if (AsciiDocElementTypes.TITLE.equals(node.getElementType())) {
          return true;
        }
        node = node.getTreeParent();
      } while (node != null);
    }
    return false;
  }

  private static boolean isSection(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      (AsciiDocElementTypes.SECTION.equals(((AsciiDocFormattingBlock) block).getNode().getElementType())
        || isChildOf(AsciiDocElementTypes.HEADING, block));
  }

  private static boolean isHeader(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      AsciiDocTokenTypes.HEADER.equals(((AsciiDocFormattingBlock) block).getNode().getElementType());
  }

  private static boolean isChildOf(IElementType element, Block block) {
    ASTNode node = ((AsciiDocFormattingBlock) block).getNode();
    do {
      if (node.getElementType() == element) {
        return true;
      }
      node = node.getTreeParent();
    } while (node != null);
    return false;
  }

  private static boolean isBullet(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      AsciiDocTokenTypes.BULLET.equals(((AsciiDocFormattingBlock) block).getNode().getElementType());
  }

  private static boolean isEnumeration(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      AsciiDocTokenTypes.ENUMERATION.equals(((AsciiDocFormattingBlock) block).getNode().getElementType());
  }

  private boolean isCallOut(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      AsciiDocTokenTypes.CALLOUT.equals(((AsciiDocFormattingBlock) block).getNode().getElementType());
  }

  private static final TokenSet TEXT_SET = TokenSet.create(AsciiDocTokenTypes.TEXT, AsciiDocTokenTypes.BOLD, AsciiDocTokenTypes.BOLDITALIC,
    AsciiDocTokenTypes.ITALIC, AsciiDocTokenTypes.DOUBLE_QUOTE, AsciiDocTokenTypes.SINGLE_QUOTE,
    AsciiDocTokenTypes.BOLD_START, AsciiDocTokenTypes.BOLD_END,
    AsciiDocTokenTypes.DOUBLEBOLD_START, AsciiDocTokenTypes.DOUBLEBOLD_END,
    AsciiDocTokenTypes.ITALIC_START, AsciiDocTokenTypes.ITALIC_END,
    AsciiDocTokenTypes.DOUBLEITALIC_START, AsciiDocTokenTypes.DOUBLEITALIC_END,
    AsciiDocTokenTypes.MONO_START, AsciiDocTokenTypes.MONO_END,
    AsciiDocTokenTypes.DOUBLEMONO_START, AsciiDocTokenTypes.DOUBLEMONO_END,
    AsciiDocTokenTypes.LT,
    AsciiDocTokenTypes.GT, AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_END, AsciiDocTokenTypes.TYPOGRAPHIC_DOUBLE_QUOTE_START,
    AsciiDocTokenTypes.LPAREN, AsciiDocTokenTypes.RPAREN,
    AsciiDocTokenTypes.MACROTEXT, AsciiDocTokenTypes.ATTR_NAME,
    AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_END, AsciiDocTokenTypes.TYPOGRAPHIC_SINGLE_QUOTE_START);

  private static boolean isPartOfSentence(Block block) {
    return block instanceof AsciiDocFormattingBlock &&
      TEXT_SET.contains(((AsciiDocFormattingBlock) block).getNode().getElementType()) &&
      !"::".equals(((AsciiDocFormattingBlock) block).getNode().getText()) && // should stay on a separate line as reformatting might create property list item
      !"--".equals(((AsciiDocFormattingBlock) block).getNode().getText()); // should stay on a separate line as it might be part of a quote
  }

  @Override
  public Indent getIndent() {
    if (myNode.getElementType() == AsciiDocTokenTypes.ENUMERATION
      || myNode.getElementType() == AsciiDocTokenTypes.BULLET
      || myNode.getElementType() == AsciiDocTokenTypes.DESCRIPTION
      || myNode.getElementType() == AsciiDocTokenTypes.DESCRIPTION_END
      || myNode.getElementType() == AsciiDocElementTypes.LINK
      || myNode.getElementType() == AsciiDocElementTypes.ATTRIBUTE_REF) {
      return Indent.getAbsoluteNoneIndent();
    }

    if (!verse && TEXT_SET.contains(myNode.getElementType())) {
      return Indent.getAbsoluteNoneIndent();
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
        return Indent.getSpaceIndent(spaces, true);
      }
    }
    return Indent.getAbsoluteNoneIndent();
  }

  @Override
  public boolean isLeaf() {
    return getSubBlocks().size() == 0;
  }
}

package org.asciidoc.intellij.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.intellij.psi.tree.ICustomParsingType;
import com.intellij.psi.tree.IReparseableElementTypeBase;
import com.intellij.util.CharTable;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.lexer.AsciiDocLazyElementType;
import org.asciidoc.intellij.lexer.AsciiDocLexer;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AsciiDocCellType extends AsciiDocLazyElementType implements IReparseableElementTypeBase, ICustomParsingType {
  public AsciiDocCellType(String cell) {
    super(cell);
  }

  @Override
  public boolean isValidReparse(@NotNull ASTNode oldNode, @NotNull ASTNode newNode) {
    return newNode.getElementType() == AsciiDocElementTypes.CELL
      && newNode.getTreeNext() != null
      && newNode.getTreeNext() instanceof PsiWhiteSpace
      && newNode.getTreeNext().getTreeNext() != null
      && newNode.getTreeNext().getTreeNext().getElementType() == AsciiDocTokenTypes.BLOCK_DELIMITER
      && newNode.getTreeNext().getTreeNext().getTreeNext() == null;
  }

  @Override
  public boolean isReparseable(@Nullable ASTNode parent, @NotNull CharSequence buffer, @NotNull Language fileLanguage, @NotNull Project project) {
    char lastChar = buffer.charAt(buffer.length() - 1);
    if (lastChar == ' ' || lastChar == '\n' || lastChar == '\t') {
      return false;
    }
    // reparseable content should contain at least some text
    if (buffer.length() == 0) {
      return false;
    }
    char firstChar = buffer.charAt(0);
    // for now, only standard tables with "|" are supported
    // cell modifiers as a prefix to the cell separator are not yet supported
    if (firstChar != '|') {
      return false;
    }
    // if a "|" occurs within the text, don't try to reparse it; let's stay safe here
    for (int i = 1; i < buffer.length(); ++i) {
      if (buffer.charAt(i) == firstChar) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  @Override
  public ASTNode parse(@NotNull CharSequence text, @NotNull CharTable table) {
    final PsiBuilderFactory factory = PsiBuilderFactory.getInstance();
    final Lexer lexer = new AsciiDocLexer();
    final PsiBuilder builder = factory.createBuilder(LanguageParserDefinitions.INSTANCE.forLanguage(AsciiDocLanguage.INSTANCE), lexer, "|===\n" + text + "\n|===");
    new AsciiDocParser().parse(AsciiDocElementTypes.CELL, builder);
    if (!builder.eof()) {
      throw new AssertionError("Unexpected token: '" + builder.getTokenText() + "'");
    }
    ASTNode node = builder.getTreeBuilt().getFirstChildNode();
    if (node != null) {
      node = node.getFirstChildNode(); // block -> delimiter
    }
    if (node != null) {
      node = node.getTreeNext(); // delimiter -> whitespace
    }
    if (node != null) {
      node = node.getTreeNext(); // whitespace -> cell
    }
    if (node != null && !Objects.equals(node.getText(), text.toString())) {
      node = null;
    }
    if (node == null) {
      node = new PsiWhiteSpaceImpl("");
    }
    return node;
  }

  @Override
  public ASTNode parseContents(@NotNull ASTNode chameleon) {
    throw new IllegalStateException("not implemented");
  }
}

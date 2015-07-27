package org.asciidoc.intellij.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.asciidoc.intellij.lexer.AsciiDocLexer;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocParserDefinition implements ParserDefinition {
  private final TokenSet WHITESPACE = TokenSet.create(AsciiDocTokenTypes.LINE_BREAK);
  private final TokenSet COMMENTS = TokenSet.create(AsciiDocTokenTypes.LINE_COMMENT, AsciiDocTokenTypes.BLOCK_COMMENT);

  @NotNull
  @Override
  public Lexer createLexer(Project project) {
    return new AsciiDocLexer();
  }

  @Override
  public PsiParser createParser(Project project) {
    return new AsciiDocParser();
  }

  @Override
  public IFileElementType getFileNodeType() {
    return AsciiDocElementTypes.FILE;
  }

  @NotNull
  @Override
  public TokenSet getWhitespaceTokens() {
    return WHITESPACE;
  }

  @NotNull
  @Override
  public TokenSet getCommentTokens() {
    return COMMENTS;
  }

  @NotNull
  @Override
  public TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }

  @NotNull
  @Override
  public PsiElement createElement(ASTNode node) {
    if (node.getElementType() == AsciiDocElementTypes.SECTION) {
      return new AsciiDocSection(node);
    }
    throw new UnsupportedOperationException("Unknown node type " + node.getElementType());
  }

  @Override
  public PsiFile createFile(FileViewProvider fileViewProvider) {
    return new AsciiDocFile(fileViewProvider);
  }

  @Override
  public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode astNode, ASTNode astNode1) {
    return SpaceRequirements.MAY;
  }
}

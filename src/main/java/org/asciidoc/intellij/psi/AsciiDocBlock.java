package org.asciidoc.intellij.psi;

import com.intellij.ide.startup.ServiceNotReadyException;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.Nullable;

import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKID;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKIDEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKIDSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCKREFTEXT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.BLOCK_COMMENT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINEIDEND;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.INLINEIDSTART;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.LINE_COMMENT;
import static org.asciidoc.intellij.lexer.AsciiDocTokenTypes.SEPARATOR;

public interface AsciiDocBlock extends PsiElement, AsciiDocSelfDescribe {

  enum Type {
    UNKNOWN,
    TABLE,
    SIDEBAR, EXAMPLE, LITERAL, PASSTHROUGH, QUOTE, BLOCKMACRO, LISTING, VERSE,
    FRONTMATTER
  }

  default Type getType() {
    return Type.UNKNOWN;
  }

  @Nullable
  default String getTitle() {
    ASTNode titleNode = getNode().findChildByType(AsciiDocElementTypes.TITLE);
    if (titleNode == null) {
      return null;
    }
    String text = titleNode.getText();
    text = text.length() >= 1 ? text.substring(1) : "";
    try {
      String resolved = AsciiDocUtil.resolveAttributes(this, text);
      if (resolved != null) {
        text = resolved;
      }
    } catch (IndexNotReadyException | ServiceNotReadyException ex) {
      // noop
    }
    return text;
  }

  TokenSet INSIGNIFICANT_TOKENS_FOR_FOLDING = TokenSet.create(
    BLOCKID, BLOCKIDEND, SEPARATOR, BLOCKREFTEXT, BLOCKIDSTART, AsciiDocElementTypes.BLOCKID,
    INLINEIDEND, INLINEIDSTART,
    TokenType.WHITE_SPACE, LINE_COMMENT, BLOCK_COMMENT
  );

  @Nullable
  default PsiElement getFirstSignificantChildForFolding() {
    PsiElement child = getFirstChild();
    while (child != null &&
      INSIGNIFICANT_TOKENS_FOR_FOLDING.contains(child.getNode().getElementType()) &&
        child.getNextSibling() != null) {
      child = child.getNextSibling();
    }
    return child;
  }

  default String getDefaultTitle() {
    return "Block";
  }

  @Override
  ASTNode getNode();

  @Nullable
  default String getStyle() {
    AsciiDocBlockAttributes attrs = PsiTreeUtil.getChildOfType(this, AsciiDocBlockAttributes.class);
    if (attrs != null) {
      return attrs.getFirstPositionalAttribute();
    }
    return null;
  }

}

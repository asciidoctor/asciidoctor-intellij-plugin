package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.parser.AsciiDocElementTypes;
import org.jetbrains.annotations.NotNull;
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
    return text.length() >= 1 ? text.substring(1) : "";
  }

  TokenSet INSIGNIFICANT_TOKENS_FOR_FOLDING = TokenSet.create(
    BLOCKID, BLOCKIDEND, SEPARATOR, BLOCKREFTEXT, BLOCKIDSTART, AsciiDocElementTypes.BLOCKID,
    INLINEIDEND, INLINEIDSTART,
    TokenType.WHITE_SPACE, LINE_COMMENT, BLOCK_COMMENT
  );

  default PsiElement getFirstSignificantChildForFolding() {
    PsiElement child = getFirstChild();
    while (child != null &&
      INSIGNIFICANT_TOKENS_FOR_FOLDING.contains(child.getNode().getElementType()) &&
        child.getNextSibling() != null) {
      child = child.getNextSibling();
    }
    return child;
  }

  @NotNull
  @Override
  default String getDescription() {
    String title = getTitle();
    String style = getStyle();
    if (title == null && style == null) {
      title = getDefaultTitle();
      if (StringUtil.isNotEmpty(title)) {
        title = "(" + title + ")";
      }
    }
    if (title == null) {
      title = "";
    }
    if (style != null) {
      return "[" + style + "]" + (title.isEmpty() ? "" : " ") + title;
    }
    return title;
  }

  default String getDefaultTitle() {
    return "Block";
  }

  @Override
  ASTNode getNode();

  @Nullable
  default String getStyle() {
    AsciiDocBlockAttributes attrs = PsiTreeUtil.findChildOfType(this, AsciiDocBlockAttributes.class);
    if (attrs != null) {
      return attrs.getFirstPositionalAttribute();
    }
    return null;
  }

}

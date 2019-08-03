package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.paths.WebReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.TokenSet;
import org.apache.commons.lang3.StringEscapeUtils;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocUrl extends ASTWrapperPsiElement {
  public AsciiDocUrl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PsiReference getReference() {
    ASTNode[] children = getNode().getChildren(TokenSet.create(AsciiDocTokenTypes.URL_LINK));
    if (children.length == 1) {
      return new WebReference(this, children[0].getTextRange().shiftRight(-getNode().getStartOffset()),
        StringEscapeUtils.unescapeHtml4(children[0].getText()));
    } else {
      children = getNode().getChildren(TokenSet.create(AsciiDocTokenTypes.URL_EMAIL));
      if (children.length == 1) {
        return new WebReference(this, children[0].getTextRange().shiftRight(-getNode().getStartOffset()),
          "mailto:" + StringEscapeUtils.unescapeHtml4(children[0].getText()));
      }
      return null;
    }
  }

  public boolean hasText() {
    ASTNode[] children = getNode().getChildren(TokenSet.create(AsciiDocTokenTypes.LINKTEXT_START));
    return children.length == 1;
  }

}

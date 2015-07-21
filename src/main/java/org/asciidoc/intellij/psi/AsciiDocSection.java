package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocSection extends ASTWrapperPsiElement {
  public AsciiDocSection(@NotNull ASTNode node) {
    super(node);
  }

  public String getTitle() {
    ASTNode heading = getNode().findChildByType(AsciiDocTokenTypes.HEADING);
    if (heading != null) {
      return trimHeadingPrefix(heading.getText());
    }
    return "<untitled>";
  }

  private static String trimHeadingPrefix(String text) {
    return StringUtil.trimLeading(text, '=').trim();
  }
}

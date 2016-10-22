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
      return trimHeading(heading.getText());
    }
    return "<untitled>";
  }

  private static String trimHeading(String text) {
    if(text.charAt(0) == '=') {
      // new style heading
      text = StringUtil.trimLeading(text, '=').trim();
    } else {
      // old style heading
      text = text.replaceAll("[-=~\\^+\n]*$", "");
    }
    return text;
  }
}

package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElementVisitor;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

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

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AsciiDocVisitor) {
      ((AsciiDocVisitor)visitor).visitSections(this);
      return;
    }

    super.accept(visitor);
  }

  private static String trimHeading(String text) {
    if (text.charAt(0) == '=') {
      // new style heading
      text = StringUtil.trimLeading(text, '=').trim();
    } else if (text.charAt(0) == '#') {
      // markdown style heading
      text = StringUtil.trimLeading(text, '#').trim();
    } else {
      // old style heading
      text = text.replaceAll("[-=~^+\n \t]*$", "");
    }
    return text;
  }

  @Override
  public String getName() {
    return getTitle();
  }

  @Override
  public ItemPresentation getPresentation() {
    return AsciiDocPsiImplUtil.getPresentation(this);
  }

  @Override
  public Icon getIcon(int ignored) {
    return AsciiDocIcons.Structure.Section;
  }
}

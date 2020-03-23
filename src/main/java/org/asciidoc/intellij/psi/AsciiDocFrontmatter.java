package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.tree.IElementType;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

public class AsciiDocFrontmatter extends AbstractAsciiDocCodeBlock {
  AsciiDocFrontmatter(IElementType type) {
    super(type);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AsciiDocVisitor) {
      ((AsciiDocVisitor) visitor).visitBlocks(this);
      return;
    }

    super.accept(visitor);
  }

  @Override
  public Type getType() {
    return Type.FRONTMATTER;
  }

  @Override
  public String getDefaultTitle() {
    return "Frontmatter";
  }

  @Override
  public TextRange getContentTextRange() {
    return getContentTextRange(AsciiDocTokenTypes.FRONTMATTER_DELIMITER);
  }

  @NotNull
  @Override
  public LiteralTextEscaper<? extends AsciiDocElementWithLanguage> createLiteralTextEscaper() {
    return new LiteralTextEscaper<AsciiDocElementWithLanguage>(this) {
      @Override
      public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
        outChars.append(rangeInsideHost.substring(myHost.getText()));
        return true;
      }

      @Override
      public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
        return rangeInsideHost.getStartOffset() + offsetInDecoded;
      }

      @NotNull
      @Override
      public TextRange getRelevantTextRange() {
        return myHost.getContentTextRange();
      }

      @Override
      public boolean isOneLine() {
        return false;
      }
    };
  }

  @Override
  public String getFenceLanguage() {
    return "source-yaml";
  }

  public static class Manipulator extends AbstractManipulator<AsciiDocFrontmatter> {

    @Override
    protected AsciiDocFrontmatter createElement(AsciiDocFrontmatter element, String content) {
      return AsciiDocPsiElementFactory.createFrontmatter(element.getProject(), content);
    }
  }

}

package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocPsiElementFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Replaces the open block delimiters (<code>--</code>) of a source or diagram block with the listing delimiter
 * (<code>----</code>) for source blocks and with the literal delimiter (<code>....</code>) for diagram blocks.
 */
public class AsciiDocConvertOpenBlockListing implements LocalQuickFix {
  private static final String LISTING_BLOCK_DELIMITER = "----";
  private static final String LITERAL_BLOCK_DELIMITER = "....";

  public static String getTargetDelimiter(PsiElement element) {
    if (element instanceof AsciiDocBlock block && "source".equals(block.getStyle())) {
      return LISTING_BLOCK_DELIMITER;
    }
    return LITERAL_BLOCK_DELIMITER;
  }

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.convertOpenBlockListing");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    final PsiElement element = descriptor.getPsiElement();
    final StringBuilder text = new StringBuilder(element.getText());
    final String base = getTargetDelimiter(element);
    StringBuilder delimiter = new StringBuilder(base);
    while (text.indexOf("\n" + delimiter) != -1) {
      delimiter.append(base.charAt(0));
    }
    // replace from the end, so that the offsets of the first delimiter stay valid
    ASTNode last = element.getNode().getLastChildNode();
    if (last != null && last.getElementType() == AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER) {
      int start = last.getStartOffset() - element.getTextRange().getStartOffset();
      text.replace(start, start + last.getTextLength(), delimiter.toString());
    } else {
      text.append("\n").append(delimiter);
    }
    ASTNode first = element.getNode().getFirstChildNode();
    while (first != null && first.getElementType() != AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER) {
      first = first.getTreeNext();
    }
    if (first != null) {
      int start = first.getStartOffset() - element.getTextRange().getStartOffset();
      text.replace(start, start + first.getTextLength(), delimiter.toString());
    }
    element.replace(AsciiDocPsiElementFactory.createListing(project, text.toString()));
  }
}

package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocPsiElementFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Fatih Bozik
 */
public class AsciiDocConvertMarkdownListing implements LocalQuickFix {
  private static final String LISTING_BLOCK_DELIMITER = "----";

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.convertMarkdownListing");
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    final PsiElement element = descriptor.getPsiElement();
    final String text = convertToAsciiDocListing(element);
    element.replace(createListing(project, text));
  }

  private String convertToAsciiDocListing(PsiElement element) {
    StringBuilder prefix = new StringBuilder();
    final StringBuilder text = new StringBuilder();
    ASTNode node  = element.getNode().getFirstChildNode();
    while (node != null && node.getElementType() != AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER) {
      prefix.append(node.getText());
      node = node.getTreeNext();
    }
    while (node != null) {
      text.append(node.getText());
      node = node.getTreeNext();
    }
    StringBuilder delimiter = new StringBuilder(LISTING_BLOCK_DELIMITER);
    while (text.indexOf("\n" + delimiter) != -1) {
      delimiter.append("-");
    }
    changeInitialDelimiter(text, element, delimiter.toString());
    changeTrailingDelimiter(text, element, delimiter.toString());
    prefix.append(text);
    return prefix.toString();
  }

  private void changeInitialDelimiter(StringBuilder text, PsiElement element, String delimiter) {
    final String language = getLanguage(element);
    final String sourceBlockName = getSourceBlockName(language, delimiter);
    final int endOffset = getEndOffset(element);
    text.replace(0, endOffset, sourceBlockName);
  }

  private int getEndOffset(PsiElement element) {
    int offset = 0;
    ASTNode node  = element.getNode().getFirstChildNode();
    while (node != null && node.getElementType() != AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER) {
      node = node.getTreeNext();
    }
    if (node == null) {
      return offset;
    }
    offset += node.getTextLength();
    node = node.getTreeNext();
    if (node == null || node.getElementType() != AsciiDocTokenTypes.LISTING_TEXT) {
      return offset;
    }
    offset += node.getTextLength();
    return offset;
  }

  private String getLanguage(PsiElement element) {
    ASTNode node  = element.getNode().getFirstChildNode();
    while (node != null && node.getElementType() != AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER) {
      node = node.getTreeNext();
    }
    if (node == null) {
      return "";
    }
    node = node.getTreeNext();
    if (node == null || node.getElementType() != AsciiDocTokenTypes.LISTING_TEXT) {
      return "";
    }
    return node.getText().trim();
  }

  private String getSourceBlockName(String language, String delimiter) {
    if (!language.isEmpty()) {
      if (language.contains("]") || language.contains(",") || language.contains("=")) {
        if (!language.contains("\"")) {
          language = "language=\"" + language + "\"";
        } else if (!language.contains("'")) {
          language = "language='" + language + "'";
        } else {
          language = "language=\"" + language.replaceAll("\"", "") + "\"";
        }
      }
    }
    return (language.isEmpty() ? "[source]\n" : String.format("[source,%s]\n", language)) + delimiter;
  }

  private void changeTrailingDelimiter(StringBuilder text, PsiElement element, String delimiter) {
    if (element.getLastChild().getNode().getElementType() == AsciiDocTokenTypes.LISTING_BLOCK_DELIMITER) {
      final int startOffset = text.length() - element.getLastChild().getTextLength();
      text.replace(startOffset, text.length(), delimiter);
    } else {
      text.append("\n").append(delimiter);
    }
  }

  private PsiElement createListing(@NotNull Project project, @NotNull String text) {
    return AsciiDocPsiElementFactory.createListing(project, text);
  }
}

package org.asciidoc.intellij.quickfix;

import com.intellij.codeInspection.LocalQuickFixBase;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.asciidoc.intellij.psi.AsciiDocPsiElementFactory;
import org.jetbrains.annotations.NotNull;

import static org.asciidoc.intellij.inspections.AsciiDocListingStyleInspection.MARKDOWN_LISTING_BLOCK_DELIMITER;

/**
 * @author Fatih Bozik
 */
public class AsciiDocConvertMarkdownListing extends LocalQuickFixBase {
  public static final String NAME = "Convert to AsciiDoc Listing";
  private static final String LISTING_BLOCK_DELIMITER = "----";
  private static final String SOURCE = "[source]\n".concat(LISTING_BLOCK_DELIMITER);
  private static final String SOURCE_WITH_LANG = "[source,%s]\n".concat(LISTING_BLOCK_DELIMITER);

  public AsciiDocConvertMarkdownListing() {
    super(NAME);
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    final PsiElement element = descriptor.getPsiElement();
    final String text = convertToAsciiDocListing(element);
    element.replace(createListing(project, text));
  }

  private String convertToAsciiDocListing(PsiElement element) {
    final StringBuilder text = new StringBuilder(element.getText());
    changeInitialDelimiter(text, element);
    changeTrailingDelimiter(text);
    return text.toString();
  }

  private void changeInitialDelimiter(StringBuilder text, PsiElement element) {
    final String language = getLanguage(element);
    final String sourceBlockName = getSourceBlockName(language);
    final int endOffset = MARKDOWN_LISTING_BLOCK_DELIMITER.length() + language.length();
    text.replace(0, endOffset, sourceBlockName);
  }

  private String getLanguage(PsiElement element) {
    return element.getFirstChild().getNextSibling().getText().trim();
  }

  private String getSourceBlockName(String language) {
    return language.isEmpty() ? SOURCE : String.format(SOURCE_WITH_LANG, language);
  }

  private void changeTrailingDelimiter(StringBuilder text) {
    final int startOffset = text.length() - MARKDOWN_LISTING_BLOCK_DELIMITER.length();
    text.replace(startOffset, text.length(), LISTING_BLOCK_DELIMITER);
  }

  private PsiElement createListing(@NotNull Project project, @NotNull String text) {
    return AsciiDocPsiElementFactory.createListing(project, text);
  }
}

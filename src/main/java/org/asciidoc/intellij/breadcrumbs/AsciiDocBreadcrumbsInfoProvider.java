package org.asciidoc.intellij.breadcrumbs;

import com.intellij.lang.Language;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocListing;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.jetbrains.annotations.NotNull;

public class AsciiDocBreadcrumbsInfoProvider implements BreadcrumbsProvider {
  private static final Language[] ourLanguages = {AsciiDocLanguage.INSTANCE};

  @Override
  public Language[] getLanguages() {
    return ourLanguages;
  }

  @Override
  public boolean acceptElement(@NotNull PsiElement e) {
    return e instanceof AsciiDocSection || e instanceof AsciiDocBlock;
  }

  @NotNull
  @Override
  public String getElementInfo(@NotNull PsiElement e) {
    String title = null;
    String type = null;
    if (e instanceof AsciiDocSection) {
      type = "Section";
      title = ((AsciiDocSection) e).getTitle();
    } else if (e instanceof AsciiDocListing) {
      type = "Listing";
      title = ((AsciiDocListing) e).getTitle();
      return title != null ? title : "block";
    } else if (e instanceof AsciiDocBlock) {
      type = "Block";
      title = ((AsciiDocBlock) e).getTitle();
    } else {
      type = "??";
    }
    if (title != null && title.length() > 0) {
      title = type + ": " + StringUtil.shortenTextWithEllipsis(title, 50, 5);
    } else {
      title = type;
    }
    return title;
  }

}

package org.asciidoc.intellij.breadcrumbs;

import com.intellij.lang.Language;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocSelfDescribe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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


  @Nullable
  @Override
  public Icon getElementIcon(@NotNull PsiElement element) {
    return element.getIcon(0);
  }

  @NotNull
  @Override
  public String getElementInfo(@NotNull PsiElement e) {
    String title = "";
    if (e instanceof AsciiDocSelfDescribe) {
      title = ((AsciiDocSelfDescribe) e).getDescription();
    }
    return StringUtil.shortenTextWithEllipsis(title, 50, 5);
  }

}

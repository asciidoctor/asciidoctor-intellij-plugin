package org.asciidoc.intellij.breadcrumbs;

import com.intellij.lang.Language;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocListing;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.structureView.AsciiDocStructureViewElement;
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
    String title = null;
    String type = "";
    if (e instanceof AsciiDocSection) {
      title = ((AsciiDocSection) e).getTitle();
    } else if (e instanceof AsciiDocListing) {
      title = ((AsciiDocListing) e).getTitle();
      if (StringUtil.isEmpty(title)) {
        String style = ((AsciiDocBlock) e).getStyle();
        if (style != null) {
          title = "[" + style +"]";
        }
      }
      return title != null ? title : "";
    } else if (e instanceof AsciiDocBlock) {
      title = ((AsciiDocBlock) e).getTitle();
      if (StringUtil.isEmpty(title)) {
        String style = ((AsciiDocBlock) e).getStyle();
        if (style != null) {
          title = "[" + style +"]";
        }
      }
      if (StringUtil.isEmpty(title) && e instanceof AsciiDocBlockMacro) {
        title = ((AsciiDocBlockMacro) e).getMacroName();
      }
    } else {
      type = "??";
    }
    if (title != null && title.length() > 0) {
      title = type + StringUtil.shortenTextWithEllipsis(title, 50, 5);
    } else {
      title = type;
    }
    return title;
  }

}

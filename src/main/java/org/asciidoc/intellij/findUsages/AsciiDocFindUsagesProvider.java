package org.asciidoc.intellij.findUsages;

import com.intellij.lang.HelpID;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.asciidoc.intellij.parser.AsciiDocWordsScanner;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclarationName;
import org.asciidoc.intellij.psi.AsciiDocBlockId;
import org.asciidoc.intellij.psi.AsciiDocIncludeTagInDocument;
import org.jetbrains.annotations.NotNull;

public class AsciiDocFindUsagesProvider implements FindUsagesProvider {

  @Override
  public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
    return psiElement instanceof PsiNamedElement;
  }

  @Override
  public String getHelpId(@NotNull PsiElement psiElement) {
    return HelpID.FIND_OTHER_USAGES;
  }

  @Override
  @NotNull
  public String getType(@NotNull PsiElement element) {
    if (element instanceof AsciiDocBlockId) {
      return "AsciiDoc ID";
    } else if (element instanceof AsciiDocAttributeDeclarationName) {
      return "AsciiDoc Attribute Name";
    } else if (element instanceof AsciiDocIncludeTagInDocument) {
      return "AsciiDoc Include Tag";
    } else {
      return "";
    }
  }

  @Override
  @NotNull
  public String getDescriptiveName(@NotNull PsiElement element) {
    String name;
    if (element instanceof PsiNamedElement) {
      name = ((PsiNamedElement) element).getName();
      if (name == null) {
        name = "???";
      }
    } else {
      name = "??? " + (element.getNode() != null ? element.getNode().getElementType() : element.getClass().getName());
    }
    return name;
  }

  @Override
  @NotNull
  public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
    return getDescriptiveName(element);
  }

  @Override
  public WordsScanner getWordsScanner() {
    return new AsciiDocWordsScanner();
  }
}

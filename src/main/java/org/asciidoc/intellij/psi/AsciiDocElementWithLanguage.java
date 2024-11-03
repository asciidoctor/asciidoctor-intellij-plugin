package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiLanguageInjectionHost;

public interface AsciiDocElementWithLanguage extends PsiLanguageInjectionHost {
  String getFenceLanguage();

  TextRange getContentTextRange();

  default boolean validateContent() {
    return true;
  }
}

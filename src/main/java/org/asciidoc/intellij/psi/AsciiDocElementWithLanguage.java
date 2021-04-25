package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiLanguageInjectionHost;

public interface AsciiDocElementWithLanguage extends PsiLanguageInjectionHost {
  String getFenceLanguage();

  /**
   * Find out if language injection is enabled.
   * Depending on the content an instance might decide not to support it (for example if the content
   * contains a preprocessor macro)
   */
  boolean isInjectionEnabled();

  TextRange getContentTextRange();
}

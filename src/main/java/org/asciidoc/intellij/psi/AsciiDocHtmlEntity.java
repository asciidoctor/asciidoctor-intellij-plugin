package org.asciidoc.intellij.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

public class AsciiDocHtmlEntity extends AsciiDocASTWrapperPsiElement {
  public AsciiDocHtmlEntity(@NotNull ASTNode node) {
    super(node);
  }

  @NotNull
  @Override
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }

  public String getDecodedText() {
    String text = getText();
    if (text.startsWith("&#x")) {
      // hex encoded unicode
      int value = Integer.parseInt(text.substring(3, text.length() - 1), 16);
      if (Character.isValidCodePoint(value)) {
        text = new String(Character.toChars(value));
      }
    } else if (text.startsWith("&#")) {
      // decimal encoded unicode
      int value = Integer.parseInt(text.substring(2, text.length() - 1));
      if (Character.isValidCodePoint(value)) {
        text = new String(Character.toChars(value));
      }
    } else {
      // encoded entities like "&amp;"
      text = StringEscapeUtils.unescapeHtml4(text);
    }
    return text;
  }

}

package org.asciidoc.intellij;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.DummyHolderFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.asciidoc.intellij.psi.AsciiDocAttributeDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AsciiDocDocumentationProvider extends AbstractDocumentationProvider {
  @Nullable
  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    if (object instanceof String) {
      return new DummyElement((String) object, psiManager);
    }
    return null;
  }

  @Nullable
  @Override
  public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file, @Nullable PsiElement contextElement) {
    if (contextElement != null && (contextElement.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_NAME ||
      contextElement.getNode().getElementType() == AsciiDocTokenTypes.ATTRIBUTE_REF)) {
      String key = contextElement.getNode().getText();
      if (AsciiDocBundle.getBuiltInAttributesList().contains(key)) {
        return new DummyElement(key, file.getManager());
      }
    }
    PsiElement lookingForAttribute = contextElement;
    while (lookingForAttribute != null) {
      if (lookingForAttribute instanceof PsiWhiteSpace && lookingForAttribute.getPrevSibling() instanceof AsciiDocAttributeDeclaration) {
        lookingForAttribute = lookingForAttribute.getPrevSibling();
      }
      if (lookingForAttribute instanceof AsciiDocAttributeDeclaration) {
        String key = ((AsciiDocAttributeDeclaration) lookingForAttribute).getAttributeName();
        if (AsciiDocBundle.getBuiltInAttributesList().contains(key)) {
          return new DummyElement(key, file.getManager());
        } else {
          break;
        }
      }
      if (lookingForAttribute instanceof PsiFile) {
        break;
      }
      lookingForAttribute = lookingForAttribute.getParent();
    }
    return super.getCustomDocumentationElement(editor, file, contextElement);
  }

  @Override
  public String generateDoc(final PsiElement element, @Nullable final PsiElement originalElement) {
    if (element instanceof DummyElement) {
      DummyElement el = (DummyElement) element;

      String defaultValue = AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + el.getKey() + ".default-value");
      defaultValue = StringEscapeUtils.escapeHtml4(defaultValue);
      String values = AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + el.getKey() + ".values");
      values = StringEscapeUtils.escapeHtml4(values);
      String html = AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + el.getKey() + ".text");
      html = StringEscapeUtils.escapeHtml4(html);
      if (values != null) {
        html += "<br/><b>" + AsciiDocBundle.message("asciidoc.attributes.values") + ":</b> " + values;
      }
      if (defaultValue != null) {
        html += "<br/><b>" + AsciiDocBundle.message("asciidoc.attributes.default-value") + ":</b> " + defaultValue;
      }
      return html;
    }
    return null;
  }

  private static class DummyElement extends FakePsiElement {
    @NotNull
    private final PsiManager myPsiManager;
    @NotNull
    private final DummyHolder myDummyHolder;
    @NotNull
    private final String key;

    DummyElement(@NotNull String key, @NotNull PsiManager psiManager) {
      myPsiManager = psiManager;
      myDummyHolder = DummyHolderFactory.createHolder(myPsiManager, null);
      while (key.endsWith(":")) {
        key = key.substring(0, key.lastIndexOf(":"));
      }
      this.key = key;
    }

    @NotNull
    public String getKey() {
      return key;
    }

    @Override
    public PsiElement getParent() {
      return myDummyHolder;
    }

    @Override
    public ItemPresentation getPresentation() {
      return new ItemPresentation() {
        @Override
        public String getPresentableText() {
          return key;
        }

        @Nullable
        @Override
        public String getLocationString() {
          return null;
        }

        @Nullable
        @Override
        public Icon getIcon(boolean unused) {
          return null;
        }
      };
    }

    @Override
    public PsiManager getManager() {
      return myPsiManager;
    }
  }
}

package org.asciidoc.intellij;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.DummyHolderFactory;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AsciiDocDocumentationProvider extends AbstractDocumentationProvider {
  @Nullable
  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    if (object instanceof String) {
      return new DummyElement((String) object, psiManager);
    }
    return null;
  }

  @Override
  public String generateDoc(final PsiElement element, @Nullable final PsiElement originalElement) {
    if (element instanceof DummyElement) {
      DummyElement el = (DummyElement) element;

      String defaultValue = AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + el.getKey() + ".default-value");
      String values = AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + el.getKey() + ".values");
      String html = AsciiDocBundle.message(AsciiDocBundle.BUILTIN_ATTRIBUTE_PREFIX + el.getKey() + ".text");
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
    @NotNull private final PsiManager myPsiManager;
    @NotNull private final DummyHolder myDummyHolder;
    @NotNull private final String key;

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
        @Nullable
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

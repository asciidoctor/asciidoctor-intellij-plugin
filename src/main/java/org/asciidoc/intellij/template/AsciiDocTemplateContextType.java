package org.asciidoc.intellij.template;

import com.intellij.codeInsight.template.EverywhereContextType;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AsciiDocTemplateContextType extends TemplateContextType {

  AsciiDocTemplateContextType(@NotNull @org.jetbrains.annotations.NonNls String id,
                              @NotNull String presentableName,
                              @Nullable Class<? extends TemplateContextType> baseContextType) {
    super(id, presentableName, baseContextType);
  }

  @Override
  public boolean isInContext(@NotNull PsiFile file, int offset) {
    if (file.getViewProvider().getBaseLanguage().isKindOf(AsciiDocLanguage.INSTANCE)) {
      PsiElement element = file.findElementAt(offset);
      return element != null && !(element instanceof PsiWhiteSpace);
    }
    return false;
  }

  public static class File extends AsciiDocTemplateContextType {
    protected File() {
      super("AsciiDoc", "AsciiDoc file", EverywhereContextType.class);
    }
  }

}

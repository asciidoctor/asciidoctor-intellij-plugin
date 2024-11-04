package org.asciidoc.intellij.template;

import com.intellij.codeInsight.template.TemplateActionContext;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.jetbrains.annotations.NotNull;

public abstract class AsciiDocTemplateContextType extends TemplateContextType {

  AsciiDocTemplateContextType(@NotNull String presentableName) {
    super(presentableName);
  }

  @Override
  public boolean isInContext(@NotNull final TemplateActionContext templateActionContext) {
    final PsiFile file = templateActionContext.getFile();
    if (file.getViewProvider().getBaseLanguage().isKindOf(AsciiDocLanguage.INSTANCE)) {
      PsiElement element = file.findElementAt(templateActionContext.getStartOffset());
      return element != null && !(element instanceof PsiWhiteSpace);
    }
    return false;
  }

  public static class File extends AsciiDocTemplateContextType {
    protected File() {
      super("AsciiDoc file");
    }
  }

}

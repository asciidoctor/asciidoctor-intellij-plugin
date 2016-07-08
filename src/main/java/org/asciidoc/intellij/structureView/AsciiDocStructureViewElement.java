package org.asciidoc.intellij.structureView;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.psi.AsciiDocBlock;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class AsciiDocStructureViewElement extends PsiTreeElementBase<PsiElement> {
  public AsciiDocStructureViewElement(PsiElement psiElement) {
    super(psiElement);
  }

  @NotNull
  @Override
  public Collection<StructureViewTreeElement> getChildrenBase() {
    List<StructureViewTreeElement> result = new ArrayList<StructureViewTreeElement>();
    for (PsiElement childElement : getElement().getChildren()) {
      if (!getPresentableElementText(childElement).isEmpty()) {
        result.add(new AsciiDocStructureViewElement(childElement));
      }
    }
    return result;
  }

  @Nullable
  @Override
  public String getPresentableText() {
    return getPresentableElementText(getElement());
  }

  private static String getPresentableElementText(PsiElement element) {
    if (element instanceof AsciiDocSection) {
      return ((AsciiDocSection)element).getTitle();
    }
    if (element instanceof PsiFile) {
      return ((PsiFile)element).getName();
    }
    if (element instanceof AsciiDocBlock) {
      AsciiDocBlock block = (AsciiDocBlock)element;
      String title = block.getTitle();
      if (title != null) {
        String style = block.getStyle();
        if (style != null) {
          return "[" + style + "] " + title;
        }
        return title;
      }
    }
    return "";
  }
}

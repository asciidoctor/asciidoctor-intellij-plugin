package org.asciidoc.intellij.structureView;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    AsciiDocSection[] sections = PsiTreeUtil.getChildrenOfType(getElement(), AsciiDocSection.class);
    if (sections == null) {
      return Collections.emptyList();
    }
    List<StructureViewTreeElement> result = new ArrayList<StructureViewTreeElement>();
    for (AsciiDocSection section : sections) {
      result.add(new AsciiDocStructureViewElement(section));
    }
    return result;
  }

  @Nullable
  @Override
  public String getPresentableText() {
    if (getElement() instanceof AsciiDocSection) {
      return ((AsciiDocSection) getElement()).getTitle();
    }
    if (getElement() instanceof PsiFile) {
      return ((PsiFile) getElement()).getName();
    }
    return "";
  }
}

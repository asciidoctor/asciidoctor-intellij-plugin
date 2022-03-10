package org.asciidoc.intellij.structureView;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.asciidoc.intellij.psi.AbstractAsciiDocCodeBlock;
import org.asciidoc.intellij.psi.AsciiDocBlockMacro;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocSelfDescribe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class AsciiDocStructureViewElement extends PsiTreeElementBase<PsiElement> {
  AsciiDocStructureViewElement(PsiElement psiElement) {
    super(psiElement);
  }

  @NotNull
  @Override
  public Collection<StructureViewTreeElement> getChildrenBase() {
    List<StructureViewTreeElement> result = new ArrayList<>();
    if (getElement() == null) {
      return result;
    }
    for (PsiElement childElement : getElement().getChildren()) {
      // a block macro might contain references to other files (for example an include or an image)
      if (childElement instanceof AsciiDocBlockMacro) {
        PsiReference[] references = childElement.getReferences();
        if (references.length > 0) {
          PsiElement resolved = references[references.length - 1].resolve();
          if (!getPresentableElementText(resolved).isEmpty()) {
            result.add(new AsciiDocStructureViewElement(resolved));
            continue;
          }
        }
      }
      if (childElement instanceof AsciiDocBlockMacro) {
        PsiReference[] references = childElement.getReferences();
        if (references.length > 0) {
          PsiElement resolved = references[references.length - 1].resolve();
          if (!getPresentableElementText(resolved).isEmpty()) {
            result.add(new AsciiDocStructureViewElement(resolved));
          }
        }
        continue;
      }
      if (!getPresentableElementText(childElement).isEmpty()) {
        result.add(new AsciiDocStructureViewElement(childElement));
      }
    }
    return result;
  }

  @NotNull
  @Override
  public ItemPresentation getPresentation() {
    return new ItemPresentation() {

      @Nullable
      @Override
      public String getPresentableText() {
        return AsciiDocStructureViewElement.this.getPresentableText();
      }

      @Nullable
      @Override
      public String getLocationString() {
        return null;
      }

      @Nullable
      @Override
      public Icon getIcon(boolean unused) {
        PsiElement element = AsciiDocStructureViewElement.this.getElement();
        if (element != null) {
          return element.getIcon(0);
        } else {
          return null;
        }
      }
    };
  }

  @Nullable
  @Override
  public String getPresentableText() {
    return getPresentableElementText(getElement());
  }

  private static String getPresentableElementText(PsiElement element) {
    if (element instanceof AsciiDocSection) {
      return ((AsciiDocSection) element).getTitle();
    }
    if (element instanceof PsiFile) {
      return ((PsiFile) element).getName();
    }
    if (element instanceof AbstractAsciiDocCodeBlock) {
      String title = ((AbstractAsciiDocCodeBlock) element).getTitle();
      if (title == null) {
        title = "(" + ((AbstractAsciiDocCodeBlock) element).getDefaultTitle() + ")";
      }
      return title;
    }
    if (element instanceof AsciiDocSelfDescribe) {
      return ((AsciiDocSelfDescribe) element).getFoldedSummary();
    }
    return "";
  }
}

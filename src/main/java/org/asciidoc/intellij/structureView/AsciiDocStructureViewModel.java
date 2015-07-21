package org.asciidoc.intellij.structureView;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocStructureViewModel extends TextEditorBasedStructureViewModel {
  protected AsciiDocStructureViewModel(@NotNull AsciiDocFile file) {
    super(file);
  }

  @NotNull
  @Override
  public StructureViewTreeElement getRoot() {
    return new AsciiDocStructureViewElement(getPsiFile());
  }
}

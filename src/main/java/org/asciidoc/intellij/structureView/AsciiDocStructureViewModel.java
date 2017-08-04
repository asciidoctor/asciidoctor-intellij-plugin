package org.asciidoc.intellij.structureView;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocStructureViewModel extends StructureViewModelBase implements
  StructureViewModel.ElementInfoProvider {
  protected AsciiDocStructureViewModel(@NotNull AsciiDocFile file) {
    super(file, new AsciiDocStructureViewElement(file));
  }

  @Override
  public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
    return false;
  }

  @Override
  public boolean isAlwaysLeaf(StructureViewTreeElement element) {
    return element instanceof AsciiDocFile;
  }
}

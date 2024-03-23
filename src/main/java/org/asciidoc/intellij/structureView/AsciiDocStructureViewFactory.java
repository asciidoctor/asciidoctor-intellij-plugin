package org.asciidoc.intellij.structureView;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.asciidoc.intellij.psi.AsciiDocFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class AsciiDocStructureViewFactory implements PsiStructureViewFactory {
  @Nullable
  @Override
  public StructureViewBuilder getStructureViewBuilder(final PsiFile psiFile) {
    // this runs in the background task, so we can access slow file indexes etc.
    AsciiDocStructureViewModel model  = new AsciiDocStructureViewModel((AsciiDocFile) psiFile);
    return new TreeBasedStructureViewBuilder() {
      @NotNull
      @Override
      public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
        // this runs in the EDT, therefore return the pre-computed elements here
        return model;
      }
    };
  }
}

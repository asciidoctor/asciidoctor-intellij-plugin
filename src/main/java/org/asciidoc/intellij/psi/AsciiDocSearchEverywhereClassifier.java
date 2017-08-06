package org.asciidoc.intellij.psi;

import com.intellij.ide.actions.SearchEverywhereClassifier;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import icons.AsciiDocIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Alexander Schwartz 2017
 */
public class AsciiDocSearchEverywhereClassifier extends DefaultPsiElementCellRenderer implements SearchEverywhereClassifier {

  @Override
  public boolean isClass(@Nullable Object o) {
    return false;
  }

  @Override
  protected Icon getIcon(PsiElement element) {
    return AsciiDocIcons.Asciidoc_Icon;
  }

  @Override
  public boolean isSymbol(@Nullable Object o) {
    return false;
  }

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    if (value instanceof AsciiDocSection) {
      return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
    return null;
  }

  @Nullable
  @Override
  public VirtualFile getVirtualFile(@NotNull Object o) {
    return null;
  }

}

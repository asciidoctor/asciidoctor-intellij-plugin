package org.asciidoc.intellij.psi;

import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiFile;
import icons.AsciiDocIcons;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AsciiDocPsiImplUtil {

  public static String getName(AsciiDocSection element) {
    return element.getTitle();
  }

  public static ItemPresentation getPresentation(final AsciiDocSection element) {
    return new ItemPresentation() {
      @Nullable
      @Override
      public String getPresentableText() {
        return element.getTitle();
      }

      @Nullable
      @Override
      public String getLocationString() {
        PsiFile containingFile = element.getContainingFile();
        return containingFile == null ? null : containingFile.getName();
      }

      @Nullable
      @Override
      public Icon getIcon(boolean unused) {
        return AsciiDocIcons.Structure.SECTION;
      }
    };
  }
}

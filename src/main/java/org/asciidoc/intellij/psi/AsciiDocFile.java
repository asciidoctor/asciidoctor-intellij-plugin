package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.asciidoc.intellij.AsciiDocLanguage;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class AsciiDocFile extends PsiFileBase {
  public AsciiDocFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, AsciiDocLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return AsciiDocFileType.INSTANCE;
  }
}

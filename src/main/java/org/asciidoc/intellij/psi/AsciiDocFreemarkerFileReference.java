package org.asciidoc.intellij.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import org.asciidoc.intellij.asciidoc.FreemarkerIncludeProcessor;
import org.asciidoc.intellij.lexer.AsciiDocFreemarker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Reference from an include or import directive in a FreeMarker template to the template file.
 */
public class AsciiDocFreemarkerFileReference extends PsiReferenceBase<PsiElement> {
  public AsciiDocFreemarkerFileReference(@NotNull PsiElement element, @NotNull TextRange range) {
    super(element, range, true);
  }

  @Override
  public @Nullable PsiElement resolve() {
    PsiFile containing = getElement().getContainingFile().getOriginalFile();
    VirtualFile current = containing.getVirtualFile();
    if (current == null || !current.isInLocalFileSystem()) {
      return null;
    }
    File currentFile = new File(current.getPath());
    File boundary = FreemarkerIncludeProcessor.findBoundary(getElement().getProject(), currentFile);
    if (boundary == null) {
      return null;
    }
    File resolved = AsciiDocFreemarker.resolve(getRangeInElement().substring(getElement().getText()), currentFile, boundary);
    if (resolved == null) {
      return null;
    }
    VirtualFile target = LocalFileSystem.getInstance().findFileByIoFile(resolved);
    if (target == null || !target.isValid()) {
      return null;
    }
    return PsiManager.getInstance(getElement().getProject()).findFile(target);
  }
}

package org.asciidoc.intellij.web;

import com.intellij.ide.browsers.OpenInBrowserRequest;
import com.intellij.ide.browsers.WebBrowserUrlProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.Url;
import org.asciidoc.intellij.editor.javafx.PreviewStaticServer;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Erik Pragt
 */
public class AsciiDocBrowserUrlProvider extends WebBrowserUrlProvider {
  @Override
  public boolean canHandleElement(OpenInBrowserRequest request) {
    return request.getFile().getFileType() instanceof AsciiDocFileType
      && isRegularFile(request.getFile())
      // LightVirtualFile is used for fragment editing; for now I don't know how to encode its location to retrieve it later
      && !(request.getVirtualFile() instanceof LightVirtualFile);
  }

  private boolean isRegularFile(PsiFile file) {
    VirtualFile vf = file.getVirtualFile();
    if (vf == null) {
      vf = file.getOriginalFile().getVirtualFile();
    }
    if (vf == null) {
      return false;
    }
    String path = vf.getCanonicalPath();
    if (path == null) {
      return false;
    }
    //noinspection RedundantIfStatement
    if (LocalFileSystem.getInstance().findFileByPath(path) == null) {
      return false;
    }
    return true;
  }

  @Nullable
  @Override
  protected Url getUrl(@NotNull OpenInBrowserRequest request, @NotNull VirtualFile file) {
    return PreviewStaticServer.getFileUrl(request, file);
  }
}

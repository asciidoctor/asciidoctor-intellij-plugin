package org.asciidoc.intellij.web;

import com.intellij.ide.browsers.OpenInBrowserRequest;
import com.intellij.ide.browsers.WebBrowserUrlProvider;
import com.intellij.openapi.vfs.VirtualFile;
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
    if (request.getFile().getFileType() instanceof AsciiDocFileType) {
      return true;
    }

    return super.canHandleElement(request);
  }

  @Nullable
  @Override
  protected Url getUrl(@NotNull OpenInBrowserRequest request, @NotNull VirtualFile file) {
    return PreviewStaticServer.getFileUrl(request, file);
  }
}
